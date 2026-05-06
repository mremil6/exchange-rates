package com.marcura.exchange.repository;

import com.marcura.exchange.domain.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByCurrencyAndRateDate(String currency, LocalDate rateDate);

    List<ExchangeRate> findAllByRateDate(LocalDate rateDate);

    @Query("SELECT MAX(e.rateDate) FROM ExchangeRate e")
    Optional<LocalDate> findLatestRateDate();

    @Query("""
           SELECT e FROM ExchangeRate e
            WHERE e.currency = :currency
              AND e.rateDate <= :asOf
            ORDER BY e.rateDate DESC
           """)
    List<ExchangeRate> findRecentByCurrency(@Param("currency") String currency,
                                            @Param("asOf") LocalDate asOf);

    @Query("""
           SELECT e FROM ExchangeRate e
            WHERE e.currency = :currency
              AND e.rateDate BETWEEN :from AND :to
            ORDER BY e.rateDate ASC
           """)
    List<ExchangeRate> findSeries(@Param("currency") String currency,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);
}
