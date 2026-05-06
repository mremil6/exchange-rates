package com.marcura.exchange.service;

import com.marcura.exchange.repository.CurrencyUsageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UsageCounterServiceTest {

    @Mock CurrencyUsageRepository totalRepo;
    @InjectMocks UsageCounterService service;

    @Test
    void recordPair_incrementsBothCurrencies() {
        service.recordPair("EUR", "PLN");

        verify(totalRepo).upsertIncrement(eq("EUR"), any(Instant.class));
        verify(totalRepo).upsertIncrement(eq("PLN"), any(Instant.class));
    }

    @Test
    void recordPair_sameCurrencyIncrementsOnce() {
        service.recordPair("EUR", "EUR");

        verify(totalRepo, times(1)).upsertIncrement(eq("EUR"), any(Instant.class));
    }

    @Test
    void recordPair_caseInsensitiveSameCurrency() {
        service.recordPair("eur", "EUR");

        verify(totalRepo, times(1)).upsertIncrement(any(), any(Instant.class));
    }
}
