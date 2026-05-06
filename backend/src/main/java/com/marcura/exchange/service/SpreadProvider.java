package com.marcura.exchange.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Maps currency codes to spread percentages per Appendix B.
 * The API base currency always carries 0%; unlisted currencies default to 2.75%.
 */
@Component
public class SpreadProvider {

    private static final BigDecimal SPREAD_BASE     = new BigDecimal("0.00");
    private static final BigDecimal SPREAD_GROUP_A  = new BigDecimal("3.25");
    private static final BigDecimal SPREAD_GROUP_B  = new BigDecimal("4.50");
    private static final BigDecimal SPREAD_GROUP_C  = new BigDecimal("6.00");
    private static final BigDecimal SPREAD_DEFAULT  = new BigDecimal("2.75");

    private static final Map<String, BigDecimal> OVERRIDES = Map.ofEntries(
        Map.entry("JPY", SPREAD_GROUP_A),
        Map.entry("HKD", SPREAD_GROUP_A),
        Map.entry("KRW", SPREAD_GROUP_A),
        Map.entry("MYR", SPREAD_GROUP_B),
        Map.entry("INR", SPREAD_GROUP_B),
        Map.entry("MXN", SPREAD_GROUP_B),
        Map.entry("RUB", SPREAD_GROUP_C),
        Map.entry("CNY", SPREAD_GROUP_C),
        Map.entry("ZAR", SPREAD_GROUP_C)
    );

    /**
     * @param currency     uppercase ISO-4217 code
     * @param baseCurrency the API base currency (carries 0% spread)
     * @return spread as a percentage value, e.g. {@code 4.00} means 4.00%
     */
    public BigDecimal spreadFor(String currency, String baseCurrency) {
        if (currency == null) {
            throw new IllegalArgumentException("currency is required");
        }
        if (currency.equalsIgnoreCase(baseCurrency)) {
            return SPREAD_BASE;
        }
        return OVERRIDES.getOrDefault(currency.toUpperCase(), SPREAD_DEFAULT);
    }

    public Set<String> knownOverrides() {
        return OVERRIDES.keySet();
    }
}
