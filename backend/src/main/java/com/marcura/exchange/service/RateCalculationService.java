package com.marcura.exchange.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Computes the spread-adjusted exchange rate:
 * <pre>adjustedRate = (toRate / fromRate) × ((100 − max(fromSpread, toSpread)) / 100)</pre>
 * {@link MathContext#DECIMAL64} prevents {@code ArithmeticException} on non-terminating
 * decimal expansions. Result is rounded to scale 10.
 */
@Service
public class RateCalculationService {

    public static final int RESULT_SCALE = 10;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final MathContext MC = MathContext.DECIMAL64;

    /**
     * @param fromRate {@code from} currency's rate against the base (e.g. EUR/USD = 0.8)
     * @param toRate   {@code to}   currency's rate against the base (e.g. PLN/USD = 3.7)
     * @param fromSpread spread % for {@code from}, e.g. 1.00 means 1%
     * @param toSpread   spread % for {@code to},   e.g. 4.00 means 4%
     */
    public BigDecimal compute(BigDecimal fromRate,
                              BigDecimal toRate,
                              BigDecimal fromSpread,
                              BigDecimal toSpread) {
        if (fromRate == null || toRate == null) {
            throw new IllegalArgumentException("rates are required");
        }
        if (fromRate.signum() <= 0) {
            throw new IllegalArgumentException("fromRate must be positive");
        }

        BigDecimal effectiveSpread = fromSpread.max(toSpread);
        BigDecimal multiplier = HUNDRED.subtract(effectiveSpread)
                                       .divide(HUNDRED, MC);

        BigDecimal raw = toRate.divide(fromRate, MC);

        return raw.multiply(multiplier, MC)
                  .setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }
}
