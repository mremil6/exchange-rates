package com.marcura.exchange.service;

import com.marcura.exchange.domain.CurrencyUsage;
import com.marcura.exchange.domain.ExchangeRate;
import com.marcura.exchange.repository.CurrencyUsageRepository;
import com.marcura.exchange.repository.ExchangeRateRepository;
import com.marcura.exchange.web.error.RateNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.marcura.exchange.web.dto.HistoricalRatesResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock ExchangeRateRepository rateRepository;
    @Mock CurrencyUsageRepository usageRepository;
    @Mock UsageCounterService usageCounterService;
    @InjectMocks ExchangeRateService service;

    SpreadProvider spreadProvider;
    RateCalculationService calc;

    @BeforeEach
    void wireRealCollaborators() {
        spreadProvider = new SpreadProvider();
        calc = new RateCalculationService();
        service = new ExchangeRateService(rateRepository, usageRepository, usageCounterService, calc, spreadProvider);
    }

    @Test
    void returnsAdjustedRateAndIncrementsCounters() {
        LocalDate d = LocalDate.parse("2024-03-15");
        when(rateRepository.findByCurrencyAndRateDate("EUR", d))
                .thenReturn(Optional.of(rate("EUR", d, "0.8")));
        when(rateRepository.findByCurrencyAndRateDate("PLN", d))
                .thenReturn(Optional.of(rate("PLN", d, "3.7")));
        when(usageRepository.findById("EUR"))
                .thenReturn(Optional.of(usage("EUR", 1)));
        when(usageRepository.findById("PLN"))
                .thenReturn(Optional.of(usage("PLN", 1)));

        ExchangeRateService.Result r = service.exchange("eur", "pln", Optional.of(d));

        assertThat(r.from()).isEqualTo("EUR");
        assertThat(r.to()).isEqualTo("PLN");
        // EUR spread=0% (base currency), PLN spread=2.75% (default) → 3.7/0.8 × 0.9725 = 4.50
        assertThat(r.exchange().setScale(2, java.math.RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("4.50"));
        assertThat(r.date()).isEqualTo(d);

        verify(usageCounterService).recordPair("EUR", "PLN");
    }

    @Test
    void usesLatestDateWhenNoneProvided() {
        LocalDate latest = LocalDate.parse("2024-03-15");
        when(rateRepository.findLatestRateDate()).thenReturn(Optional.of(latest));
        when(rateRepository.findByCurrencyAndRateDate("EUR", latest))
                .thenReturn(Optional.of(rate("EUR", latest, "1.00")));
        when(rateRepository.findByCurrencyAndRateDate("USD", latest))
                .thenReturn(Optional.of(rate("USD", latest, "1.08")));
        when(usageRepository.findById(anyString()))
                .thenReturn(Optional.of(usage("X", 0)));

        ExchangeRateService.Result r = service.exchange("EUR", "USD", Optional.empty());
        assertThat(r.date()).isEqualTo(latest);
    }

    @Test
    void throws404WhenRateMissing() {
        LocalDate d = LocalDate.parse("2024-03-15");
        when(rateRepository.findByCurrencyAndRateDate("EUR", d))
                .thenReturn(Optional.of(rate("EUR", d, "0.8")));
        when(rateRepository.findByCurrencyAndRateDate("PLN", d))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exchange("EUR", "PLN", Optional.of(d)))
                .isInstanceOf(RateNotFoundException.class);
    }

    @Test
    void rejectsBadCurrencyCode() {
        assertThatThrownBy(() -> service.exchange("EU", "USD", Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void historicalAdjusted_returnsSpreadAdjustedPoints() {
        LocalDate d1 = LocalDate.parse("2024-03-01");
        LocalDate d2 = LocalDate.parse("2024-03-02");
        LocalDate from = LocalDate.parse("2024-03-01");
        LocalDate to = LocalDate.parse("2024-03-07");

        when(rateRepository.findSeries(eq("EUR"), eq(from), eq(to)))
                .thenReturn(List.of(rate("EUR", d1, "1.00"), rate("EUR", d2, "1.00")));
        when(rateRepository.findSeries(eq("USD"), eq(from), eq(to)))
                .thenReturn(List.of(rate("USD", d1, "1.08"), rate("USD", d2, "1.09")));

        HistoricalRatesResponse resp = service.historicalAdjusted("EUR", "USD", from, to);

        assertThat(resp.from()).isEqualTo("EUR");
        assertThat(resp.to()).isEqualTo("USD");
        assertThat(resp.points()).hasSize(2);
        assertThat(resp.points()).allMatch(p -> p.rate().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void historicalAdjusted_throws404WhenFromSeriesEmpty() {
        LocalDate from = LocalDate.parse("2024-03-01");
        LocalDate to = LocalDate.parse("2024-03-07");

        when(rateRepository.findSeries(eq("EUR"), any(), any())).thenReturn(List.of());
        when(rateRepository.findSeries(eq("USD"), any(), any())).thenReturn(List.of(rate("USD", from, "1.08")));

        assertThatThrownBy(() -> service.historicalAdjusted("EUR", "USD", from, to))
                .isInstanceOf(RateNotFoundException.class);
    }

    @Test
    void historicalAdjusted_throws404WhenToSeriesEmpty() {
        LocalDate from = LocalDate.parse("2024-03-01");
        LocalDate to = LocalDate.parse("2024-03-07");

        when(rateRepository.findSeries(eq("EUR"), any(), any())).thenReturn(List.of(rate("EUR", from, "1.00")));
        when(rateRepository.findSeries(eq("USD"), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.historicalAdjusted("EUR", "USD", from, to))
                .isInstanceOf(RateNotFoundException.class);
    }

    @Test
    void historicalSeries_delegatesToRepository() {
        LocalDate from = LocalDate.parse("2024-03-01");
        LocalDate to = LocalDate.parse("2024-03-07");
        List<com.marcura.exchange.domain.ExchangeRate> rows = List.of(rate("EUR", from, "1.00"));
        when(rateRepository.findSeries("EUR", from, to)).thenReturn(rows);

        List<com.marcura.exchange.domain.ExchangeRate> result = service.historicalSeries("eur", from, to);

        assertThat(result).isSameAs(rows);
    }

    private ExchangeRate rate(String ccy, LocalDate d, String rate) {
        return ExchangeRate.builder()
                .currency(ccy).rateDate(d).rateToBase(new BigDecimal(rate))
                .baseCurrency("EUR").fetchedAt(Instant.EPOCH).build();
    }

    private CurrencyUsage usage(String ccy, long n) {
        return CurrencyUsage.builder().currency(ccy).totalCount(n).lastQueriedAt(Instant.EPOCH).build();
    }
}
