package com.marcura.exchange.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One unit of {@link #baseCurrency} expressed in {@link #currency} on {@link #rateDate}.
 * Persisted to scale 8; computation uses scale 10.
 * The unique constraint on {@code (currency, rate_date)} enforces upsert idempotency.
 */
@Entity
@Table(
    name = "exchange_rate",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_exchange_rate_currency_date",
        columnNames = {"currency", "rate_date"}
    ),
    indexes = {
        @Index(name = "idx_exchange_rate_date", columnList = "rate_date DESC"),
        @Index(name = "idx_exchange_rate_currency", columnList = "currency")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"currency", "rateDate"})
@ToString
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(name = "rate_to_base", nullable = false, precision = 19, scale = 8)
    private BigDecimal rateToBase;

    @Column(name = "base_currency", nullable = false, length = 8)
    private String baseCurrency;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;
}
