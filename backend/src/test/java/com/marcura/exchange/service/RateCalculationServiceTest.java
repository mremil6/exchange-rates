package com.marcura.exchange.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateCalculationServiceTest {

    private final RateCalculationService service = new RateCalculationService();

    @Test
    @DisplayName("Worked example from the brief: EUR(0.8, 1%) -> PLN(3.7, 4%) = 4.44")
    void workedExample() {
        BigDecimal result = service.compute(
                new BigDecimal("0.8"),
                new BigDecimal("3.7"),
                new BigDecimal("1.00"),
                new BigDecimal("4.00"));

        assertThat(result.setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("4.44"));
    }

    @Test
    @DisplayName("Identical currencies → 1.0 minus the spread haircut")
    void sameCurrency() {
        BigDecimal result = service.compute(
                new BigDecimal("1.0"),
                new BigDecimal("1.0"),
                new BigDecimal("2.75"),
                new BigDecimal("2.75"));
        assertThat(result.setScale(4, RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("0.9725"));
    }

    @Test
    @DisplayName("Higher spread of the pair is the one applied")
    void picksMaxSpread() {
        BigDecimal a = service.compute(
                new BigDecimal("0.8"), new BigDecimal("3.7"),
                new BigDecimal("4.00"), new BigDecimal("1.00"));
        BigDecimal b = service.compute(
                new BigDecimal("0.8"), new BigDecimal("3.7"),
                new BigDecimal("1.00"), new BigDecimal("4.00"));
        assertThat(a).isEqualByComparingTo(b);
    }

    @ParameterizedTest(name = "[{index}] {0}/{1} @ ({2},{3}) spread ({4},{5}) → {6}")
    @MethodSource("scenarios")
    void parameterised(String fromCcy, String toCcy,
                       BigDecimal fromRate, BigDecimal toRate,
                       BigDecimal fromSpread, BigDecimal toSpread,
                       BigDecimal expectedScale4) {
        BigDecimal got = service.compute(fromRate, toRate, fromSpread, toSpread)
                                .setScale(4, RoundingMode.HALF_UP);
        assertThat(got).isEqualByComparingTo(expectedScale4);
    }

    static Stream<Arguments> scenarios() {
        return Stream.of(
            Arguments.of("EUR", "PLN",
                    new BigDecimal("0.8"), new BigDecimal("3.7"),
                    new BigDecimal("1.00"), new BigDecimal("4.00"),
                    new BigDecimal("4.4400")),
            Arguments.of("PLN", "EUR",
                    new BigDecimal("3.7"), new BigDecimal("0.8"),
                    new BigDecimal("4.00"), new BigDecimal("1.00"),
                    new BigDecimal("0.2076")),
            Arguments.of("USD", "GBP",
                    new BigDecimal("1.0"), new BigDecimal("0.79"),
                    new BigDecimal("2.75"), new BigDecimal("2.75"),
                    new BigDecimal("0.7683"))
        );
    }

    @Test
    @DisplayName("Throws on null or non-positive fromRate")
    void rejectsBadInputs() {
        assertThatThrownBy(() -> service.compute(null, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.compute(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
