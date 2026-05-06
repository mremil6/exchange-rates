package com.marcura.exchange.service.ingestion;

import com.marcura.exchange.domain.ExchangeRate;
import com.marcura.exchange.repository.ExchangeRateRepository;
import com.marcura.exchange.service.fixer.FixerClient;
import com.marcura.exchange.service.fixer.FixerLatestResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Fetches rates from Fixer.io and persists them idempotently.
 * Re-ingesting the same day refreshes {@code rateToBase} and {@code fetchedAt} only;
 * the {@code (currency, rate_date)} unique constraint prevents duplicate rows.
 * {@code rateDate} is taken from the API response, never from the system clock.
 */
@Service
@RequiredArgsConstructor
public class RateIngestionService {

    private static final Logger log = LoggerFactory.getLogger(RateIngestionService.class);

    private final FixerClient fixerClient;
    private final ExchangeRateRepository rateRepository;
    private final SeedDataService seedDataService;

    /**
     * @return number of rate rows written or refreshed
     */
    @Transactional
    public int ingestLatest() {
        Optional<FixerLatestResponse> response = fixerClient.fetchLatest();
        if (response.isEmpty()) {
            if (!fixerClient.isConfigured()) {
                log.info("No FIXER_API_KEY configured; falling back to seed data");
                return seedDataService.seedIfEmpty();
            }
            log.warn("Fixer returned no usable response; skipping ingest");
            return 0;
        }
        return persist(response.get());
    }

    @Transactional
    public int ingestForDate(LocalDate date) {
        return fixerClient.fetchForDate(date)
                .map(this::persist)
                .orElse(0);
    }

    private int persist(FixerLatestResponse resp) {
        LocalDate rateDate = resp.date();
        String base = resp.base();
        Instant now = Instant.now();
        int written = 0;

        for (Map.Entry<String, BigDecimal> e : resp.rates().entrySet()) {
            String currency = e.getKey();
            BigDecimal rate = e.getValue();
            if (rate == null || rate.signum() <= 0) {
                continue;
            }
            ExchangeRate row = rateRepository
                    .findByCurrencyAndRateDate(currency, rateDate)
                    .orElseGet(() -> ExchangeRate.builder()
                            .currency(currency)
                            .rateDate(rateDate)
                            .baseCurrency(base)
                            .build());
            row.setRateToBase(rate);
            row.setBaseCurrency(base);
            row.setFetchedAt(now);
            rateRepository.save(row);
            written++;
        }
        log.info("Ingested {} rates for {} (base={})", written, rateDate, base);
        return written;
    }
}
