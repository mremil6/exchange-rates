package com.marcura.exchange.service.fixer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Subset of the Fixer.io {@code /latest} (and dated) response.
 * We deliberately ignore unknown fields so adding new ones upstream doesn't break us.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FixerLatestResponse(
        boolean success,
        Long timestamp,
        String base,
        LocalDate date,
        Map<String, BigDecimal> rates,
        FixerError error
) {
    public boolean isOk() {
        return success && rates != null && !rates.isEmpty() && date != null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FixerError(int code, String type, String info) {}
}
