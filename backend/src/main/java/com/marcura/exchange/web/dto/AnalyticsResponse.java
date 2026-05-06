package com.marcura.exchange.web.dto;

import java.time.Instant;
import java.util.List;

public record AnalyticsResponse(List<TopCurrency> topCurrencies) {
    public record TopCurrency(String currency, long totalCount, Instant lastQueriedAt) {}
}
