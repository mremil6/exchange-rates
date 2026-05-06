package com.marcura.exchange.service;

import com.marcura.exchange.domain.ExchangeRate;
import com.marcura.exchange.repository.ExchangeRateRepository;
import com.marcura.exchange.service.fixer.FixerClient;
import com.marcura.exchange.service.fixer.FixerLatestResponse;
import com.marcura.exchange.service.ingestion.RateIngestionService;
import com.marcura.exchange.service.ingestion.SeedDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateIngestionServiceTest {

    @Mock FixerClient fixerClient;
    @Mock ExchangeRateRepository rateRepository;
    @Mock SeedDataService seedDataService;
    @InjectMocks RateIngestionService service;

    @Test
    void ingestLatest_fallsBackToSeedWhenNoApiKey() {
        when(fixerClient.fetchLatest()).thenReturn(Optional.empty());
        when(fixerClient.isConfigured()).thenReturn(false);
        when(seedDataService.seedIfEmpty()).thenReturn(30);

        int result = service.ingestLatest();

        assertThat(result).isEqualTo(30);
        verify(seedDataService).seedIfEmpty();
    }

    @Test
    void ingestLatest_returnsZeroWhenFixerFailsAndIsConfigured() {
        when(fixerClient.fetchLatest()).thenReturn(Optional.empty());
        when(fixerClient.isConfigured()).thenReturn(true);

        int result = service.ingestLatest();

        assertThat(result).isEqualTo(0);
        verify(seedDataService, never()).seedIfEmpty();
    }

    @Test
    void ingestLatest_persistsRatesFromResponse() {
        LocalDate date = LocalDate.parse("2024-03-15");
        FixerLatestResponse resp = new FixerLatestResponse(true, 123L, "EUR", date,
                Map.of("USD", new BigDecimal("1.08")), null);
        when(fixerClient.fetchLatest()).thenReturn(Optional.of(resp));
        when(rateRepository.findByCurrencyAndRateDate("USD", date)).thenReturn(Optional.empty());

        int result = service.ingestLatest();

        assertThat(result).isEqualTo(1);
        verify(rateRepository).save(any(ExchangeRate.class));
    }

    @Test
    void ingestLatest_updatesExistingRow() {
        LocalDate date = LocalDate.parse("2024-03-15");
        ExchangeRate existing = ExchangeRate.builder()
                .currency("USD").rateDate(date).baseCurrency("EUR")
                .rateToBase(new BigDecimal("1.07")).fetchedAt(Instant.EPOCH).build();
        FixerLatestResponse resp = new FixerLatestResponse(true, 123L, "EUR", date,
                Map.of("USD", new BigDecimal("1.08")), null);
        when(fixerClient.fetchLatest()).thenReturn(Optional.of(resp));
        when(rateRepository.findByCurrencyAndRateDate("USD", date)).thenReturn(Optional.of(existing));

        service.ingestLatest();

        verify(rateRepository).save(existing);
        assertThat(existing.getRateToBase()).isEqualByComparingTo("1.08");
    }

    @Test
    void ingestLatest_skipsNullOrNegativeRates() {
        LocalDate date = LocalDate.parse("2024-03-15");
        FixerLatestResponse resp = new FixerLatestResponse(true, 123L, "EUR", date,
                Map.of("BAD", BigDecimal.ZERO, "NEG", new BigDecimal("-1.0")), null);
        when(fixerClient.fetchLatest()).thenReturn(Optional.of(resp));

        int result = service.ingestLatest();

        assertThat(result).isEqualTo(0);
        verify(rateRepository, never()).save(any());
    }

    @Test
    void ingestForDate_returnsZeroWhenFixerReturnsEmpty() {
        LocalDate date = LocalDate.parse("2024-03-15");
        when(fixerClient.fetchForDate(date)).thenReturn(Optional.empty());

        assertThat(service.ingestForDate(date)).isEqualTo(0);
    }

    @Test
    void ingestForDate_persistsRatesWhenPresent() {
        LocalDate date = LocalDate.parse("2024-03-15");
        FixerLatestResponse resp = new FixerLatestResponse(true, 123L, "EUR", date,
                Map.of("GBP", new BigDecimal("0.86")), null);
        when(fixerClient.fetchForDate(date)).thenReturn(Optional.of(resp));
        when(rateRepository.findByCurrencyAndRateDate("GBP", date)).thenReturn(Optional.empty());

        int result = service.ingestForDate(date);

        assertThat(result).isEqualTo(1);
    }
}
