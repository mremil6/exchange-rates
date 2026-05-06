package com.marcura.exchange.service;

import com.marcura.exchange.domain.ExchangeRate;
import com.marcura.exchange.repository.CurrencyUsageRepository;
import com.marcura.exchange.repository.ExchangeRateRepository;
import com.marcura.exchange.web.dto.HistoricalRatesResponse;
import com.marcura.exchange.web.error.RateNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates spread-adjusted exchange queries: resolves the effective rate date,
 * loads both currency legs from the DB, applies spread, and records usage.
 */
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository rateRepository;
    private final CurrencyUsageRepository usageRepository;
    private final UsageCounterService usageCounterService;
    private final RateCalculationService calculationService;
    private final SpreadProvider spreadProvider;

    public record Result(
            String from,
            String to,
            BigDecimal exchange,
            LocalDate date,
            long fromQueryCount,
            long toQueryCount
    ) {}

    @Transactional
    public Result exchange(String from, String to, Optional<LocalDate> dateOpt) {
        String fromUpper = normalise(from);
        String toUpper = normalise(to);

        LocalDate effectiveDate = dateOpt.orElseGet(() -> rateRepository.findLatestRateDate()
                .orElseThrow(() -> new RateNotFoundException(
                        "No exchange rates available; ingest job has not run yet")));

        ExchangeRate fromRate = loadRateOrThrow(fromUpper, effectiveDate);
        ExchangeRate toRate = loadRateOrThrow(toUpper, effectiveDate);

        BigDecimal fromSpread = spreadProvider.spreadFor(fromUpper, fromRate.getBaseCurrency());
        BigDecimal toSpread = spreadProvider.spreadFor(toUpper, toRate.getBaseCurrency());

        BigDecimal adjusted = calculationService.compute(
                fromRate.getRateToBase(),
                toRate.getRateToBase(),
                fromSpread,
                toSpread
        );

        usageCounterService.recordPair(fromUpper, toUpper);

        long fromCount = usageRepository.findById(fromUpper)
                .map(u -> u.getTotalCount()).orElse(0L);
        long toCount = usageRepository.findById(toUpper)
                .map(u -> u.getTotalCount()).orElse(0L);

        return new Result(fromUpper, toUpper, adjusted, effectiveDate, fromCount, toCount);
    }

    @Transactional(readOnly = true)
    public List<ExchangeRate> historicalSeries(String currency, LocalDate from, LocalDate to) {
        return rateRepository.findSeries(normalise(currency), from, to);
    }

    /**
     * Builds the spread-adjusted rate series for a currency pair over a date range.
     * Shared by the historical and insight endpoints to avoid duplicating the computation.
     */
    @Transactional(readOnly = true)
    public HistoricalRatesResponse historicalAdjusted(String from, String to,
                                                      LocalDate fromDate, LocalDate toDate) {
        String fromUpper = normalise(from);
        String toUpper = normalise(to);

        List<ExchangeRate> fromSeries = rateRepository.findSeries(fromUpper, fromDate, toDate);
        List<ExchangeRate> toSeries   = rateRepository.findSeries(toUpper,   fromDate, toDate);

        if (fromSeries.isEmpty() || toSeries.isEmpty()) {
            throw new RateNotFoundException(
                    "No historical rates for %s/%s between %s and %s"
                            .formatted(fromUpper, toUpper, fromDate, toDate));
        }

        Map<LocalDate, ExchangeRate> fromByDate = index(fromSeries);
        Map<LocalDate, ExchangeRate> toByDate   = index(toSeries);

        BigDecimal fromSpread = spreadProvider.spreadFor(fromUpper, fromSeries.get(0).getBaseCurrency());
        BigDecimal toSpread   = spreadProvider.spreadFor(toUpper,   toSeries.get(0).getBaseCurrency());

        List<HistoricalRatesResponse.Point> points = fromByDate.keySet().stream()
                .filter(toByDate::containsKey)
                .sorted()
                .map(d -> new HistoricalRatesResponse.Point(
                        d,
                        calculationService.compute(
                                fromByDate.get(d).getRateToBase(),
                                toByDate.get(d).getRateToBase(),
                                fromSpread,
                                toSpread)))
                .toList();

        return new HistoricalRatesResponse(fromUpper, toUpper, fromDate, toDate, points);
    }

    private static Map<LocalDate, ExchangeRate> index(List<ExchangeRate> series) {
        Map<LocalDate, ExchangeRate> map = new HashMap<>(series.size());
        for (ExchangeRate r : series) map.put(r.getRateDate(), r);
        return map;
    }

    private ExchangeRate loadRateOrThrow(String currency, LocalDate date) {
        return rateRepository.findByCurrencyAndRateDate(currency, date)
                .orElseThrow(() -> new RateNotFoundException(
                        "No rate for %s on %s".formatted(currency, date)));
    }

    private static String normalise(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("currency code is required");
        }
        String upper = code.trim().toUpperCase();
        if (upper.length() != 3) {
            throw new IllegalArgumentException("currency code must be 3 letters: " + code);
        }
        return upper;
    }
}
