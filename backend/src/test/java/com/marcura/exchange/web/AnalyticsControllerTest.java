package com.marcura.exchange.web;

import com.marcura.exchange.domain.CurrencyUsage;
import com.marcura.exchange.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired MockMvc mvc;
    @MockBean AnalyticsService service;

    @Test
    void analytics_returnsTopCurrencies() throws Exception {
        CurrencyUsage eur = CurrencyUsage.builder()
                .currency("EUR").totalCount(42L).lastQueriedAt(Instant.EPOCH).build();
        when(service.snapshot()).thenReturn(new AnalyticsService.Snapshot(List.of(eur)));

        mvc.perform(get("/analytics"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.topCurrencies[0].currency").value("EUR"))
           .andExpect(jsonPath("$.topCurrencies[0].totalCount").value(42));
    }

    @Test
    void analytics_returnsEmptyListWhenNoUsage() throws Exception {
        when(service.snapshot()).thenReturn(new AnalyticsService.Snapshot(List.of()));

        mvc.perform(get("/analytics"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.topCurrencies").isArray())
           .andExpect(jsonPath("$.topCurrencies").isEmpty());
    }
}
