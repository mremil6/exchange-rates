package com.marcura.exchange.web;

import com.marcura.exchange.service.ExchangeRateService;
import com.marcura.exchange.service.insight.TrendInsightService;
import com.marcura.exchange.web.dto.HistoricalRatesResponse;
import com.marcura.exchange.web.error.RateNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InsightController.class)
class InsightControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ExchangeRateService exchangeRateService;
    @MockBean TrendInsightService insightService;

    @Test
    void insight_returnsInsightText() throws Exception {
        LocalDate d1 = LocalDate.parse("2024-03-01");
        LocalDate d2 = LocalDate.parse("2024-03-07");
        HistoricalRatesResponse series = new HistoricalRatesResponse("EUR", "USD", d1, d2,
                List.of(new HistoricalRatesResponse.Point(d1, new BigDecimal("1.08"))));
        when(exchangeRateService.historicalAdjusted(eq("EUR"), eq("USD"), any(), any()))
                .thenReturn(series);
        when(insightService.generate(series)).thenReturn("EUR/USD rose 2% this week.");

        mvc.perform(get("/exchange/insight")
                .param("from", "EUR").param("to", "USD")
                .param("fromDate", "2024-03-01").param("toDate", "2024-03-07"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.from").value("EUR"))
           .andExpect(jsonPath("$.insight").value("EUR/USD rose 2% this week."));
    }

    @Test
    void insight_returns400WhenToDateBeforeFromDate() throws Exception {
        mvc.perform(get("/exchange/insight")
                .param("from", "EUR").param("to", "USD")
                .param("fromDate", "2024-03-07").param("toDate", "2024-03-01"))
           .andExpect(status().isBadRequest());
    }

    @Test
    void insight_returns404WhenNoRates() throws Exception {
        when(exchangeRateService.historicalAdjusted(any(), any(), any(), any()))
                .thenThrow(new RateNotFoundException("No rates"));

        mvc.perform(get("/exchange/insight")
                .param("from", "EUR").param("to", "USD")
                .param("fromDate", "2024-03-01").param("toDate", "2024-03-07"))
           .andExpect(status().isNotFound());
    }
}
