package com.marcura.exchange.service;

import com.marcura.exchange.domain.CurrencyUsage;
import com.marcura.exchange.repository.CurrencyUsageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock CurrencyUsageRepository totalRepo;
    @InjectMocks AnalyticsService service;

    @Test
    void snapshot_returnsTopCurrencies() {
        CurrencyUsage eur = CurrencyUsage.builder()
                .currency("EUR").totalCount(42L).lastQueriedAt(Instant.EPOCH).build();
        when(totalRepo.findAllOrderByCountDesc()).thenReturn(List.of(eur));

        AnalyticsService.Snapshot snap = service.snapshot();

        assertThat(snap.top()).hasSize(1);
        assertThat(snap.top().get(0).getCurrency()).isEqualTo("EUR");
        verify(totalRepo).findAllOrderByCountDesc();
    }

    @Test
    void snapshot_returnsEmptyListWhenNoUsage() {
        when(totalRepo.findAllOrderByCountDesc()).thenReturn(List.of());

        assertThat(service.snapshot().top()).isEmpty();
    }
}
