package com.marcura.exchange.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Per-currency aggregate query counter. Incremented via a single SQL upsert
 * to ensure atomicity across threads and instances.
 */
@Entity
@Table(name = "currency_usage")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CurrencyUsage {

    @Id
    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "total_count", nullable = false)
    private long totalCount;

    @Column(name = "last_queried_at")
    private Instant lastQueriedAt;
}
