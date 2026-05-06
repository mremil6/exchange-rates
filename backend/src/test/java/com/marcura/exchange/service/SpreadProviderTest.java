package com.marcura.exchange.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpreadProviderTest {

    private final SpreadProvider provider = new SpreadProvider();

    @Test
    void baseCurrencyHasZeroSpread() {
        assertThat(provider.spreadFor("EUR", "EUR"))
                .isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void groupASpread() {
        assertThat(provider.spreadFor("JPY", "EUR"))
                .isEqualByComparingTo(new BigDecimal("3.25"));
        assertThat(provider.spreadFor("HKD", "EUR"))
                .isEqualByComparingTo(new BigDecimal("3.25"));
        assertThat(provider.spreadFor("KRW", "EUR"))
                .isEqualByComparingTo(new BigDecimal("3.25"));
    }

    @Test
    void groupBSpread() {
        assertThat(provider.spreadFor("INR", "EUR"))
                .isEqualByComparingTo(new BigDecimal("4.50"));
    }

    @Test
    void groupCSpread() {
        assertThat(provider.spreadFor("RUB", "EUR"))
                .isEqualByComparingTo(new BigDecimal("6.00"));
    }

    @Test
    void defaultSpreadForUnknown() {
        assertThat(provider.spreadFor("PLN", "EUR"))
                .isEqualByComparingTo(new BigDecimal("2.75"));
        assertThat(provider.spreadFor("USD", "EUR"))
                .isEqualByComparingTo(new BigDecimal("2.75"));
    }

    @Test
    void rejectsNullCurrency() {
        assertThatThrownBy(() -> provider.spreadFor(null, "EUR"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
