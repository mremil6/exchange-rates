package com.marcura.exchange.repository;

import com.marcura.exchange.domain.CurrencyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CurrencyUsageRepository extends JpaRepository<CurrencyUsage, String> {

    @Modifying
    @Query(value = """
           INSERT INTO currency_usage (currency, total_count, last_queried_at)
           VALUES (:currency, 1, :ts)
           ON CONFLICT (currency) DO UPDATE
              SET total_count = currency_usage.total_count + 1,
                  last_queried_at = EXCLUDED.last_queried_at
           """, nativeQuery = true)
    void upsertIncrement(@Param("currency") String currency, @Param("ts") Instant ts);

    @Query("""
           SELECT u FROM CurrencyUsage u
            ORDER BY u.totalCount DESC
           """)
    List<CurrencyUsage> findAllOrderByCountDesc();
}
