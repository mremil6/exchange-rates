package com.marcura.exchange.service;

import com.marcura.exchange.repository.CurrencyUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Increments per-currency usage counters via a single Postgres upsert,
 * ensuring atomicity across concurrent threads and instances without application-level locking.
 */
@Service
@RequiredArgsConstructor
public class UsageCounterService {

    private final CurrencyUsageRepository totalRepo;
    private final Clock clock = Clock.systemUTC();

    /**
     * Records a query for both currencies in a single transaction so neither leg can be lost.
     */
    @Transactional
    public void recordPair(String fromCurrency, String toCurrency) {
        Instant now = clock.instant();
        totalRepo.upsertIncrement(fromCurrency, now);
        if (!fromCurrency.equalsIgnoreCase(toCurrency)) {
            totalRepo.upsertIncrement(toCurrency, now);
        }
    }
}
