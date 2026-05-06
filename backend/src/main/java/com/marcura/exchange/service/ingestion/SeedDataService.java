package com.marcura.exchange.service.ingestion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcura.exchange.domain.ExchangeRate;
import com.marcura.exchange.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Populates synthetic rate history on first boot when no Fixer API key is configured.
 * A seeded RNG keeps chart values stable across restarts.
 */
@Service
public class SeedDataService {

    private static final Logger log = LoggerFactory.getLogger(SeedDataService.class);
    private static final String BASE = "EUR";
    private static final long SEED = 0xC0FFEECAFEL;

    private final ExchangeRateRepository rateRepository;
    private final Map<String, BigDecimal> anchorRates;

    public SeedDataService(ExchangeRateRepository rateRepository, ObjectMapper objectMapper) {
        this.rateRepository = rateRepository;
        try {
            ClassPathResource resource = new ClassPathResource("seed-anchor-rates.json");
            this.anchorRates = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<Map<String, BigDecimal>>() {}
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load seed-anchor-rates.json", e);
        }
    }

    @Value("${app.ingestion.seed-days:30}")
    private int seedDays;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onReady() {
        if (rateRepository.count() > 0) {
            log.info("Rates table is not empty; skipping seed");
            return;
        }
        int n = seedIfEmpty();
        log.info("Seeded {} synthetic rate rows across {} days", n, seedDays);
    }

    @Transactional
    public int seedIfEmpty() {
        if (rateRepository.count() > 0) {
            return 0;
        }
        List<ExchangeRate> rows = generateRows();
        rateRepository.saveAll(rows);
        return rows.size();
    }

    private List<ExchangeRate> generateRows() {
        Random rng = new Random(SEED);
        Instant fetchedAt = Instant.now();
        LocalDate today = LocalDate.now();
        List<ExchangeRate> all = new java.util.ArrayList<>(anchorRates.size() * seedDays);

        for (int dayBack = seedDays - 1; dayBack >= 0; dayBack--) {
            LocalDate date = today.minusDays(dayBack);
            for (Map.Entry<String, BigDecimal> e : anchorRates.entrySet()) {
                String currency = e.getKey();
                BigDecimal anchor = e.getValue();
                double noise = (rng.nextGaussian() * 0.005);
                BigDecimal rate = anchor
                        .multiply(BigDecimal.valueOf(1 + noise))
                        .setScale(8, RoundingMode.HALF_UP);
                all.add(ExchangeRate.builder()
                        .currency(currency)
                        .rateDate(date)
                        .baseCurrency(BASE)
                        .rateToBase(rate)
                        .fetchedAt(fetchedAt)
                        .build());
            }
        }
        return all;
    }
}
