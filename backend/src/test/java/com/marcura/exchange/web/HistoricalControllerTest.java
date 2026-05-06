package com.marcura.exchange.web;

import com.marcura.exchange.service.ExchangeRateService;
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

@WebMvcTest(HistoricalController.class)
class HistoricalControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ExchangeRateService exchangeRateService;

    @Test
    void historical_returnsPoints() throws Exception {
        LocalDate d = LocalDate.parse("2024-03-01");
        HistoricalRatesResponse resp = new HistoricalRatesResponse("EUR", "USD", d,
                LocalDate.parse("2024-03-07"),
                List.of(new HistoricalRatesResponse.Point(d, new BigDecimal("1.0825"))));
        when(exchangeRateService.historicalAdjusted(eq("EUR"), eq("USD"), any(), any()))
                .thenReturn(resp);

        mvc.perform(get("/historical")
                .param("from", "EUR").param("to", "USD")
                .param("fromDate", "2024-03-01").param("toDate", "2024-03-07"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.from").value("EUR"))
           .andExpect(jsonPath("$.to").value("USD"))
           .andExpect(jsonPath("$.points[0].rate").value(1.0825));
    }

    @Test
    void historical_returns400WhenToDateBeforeFromDate() throws Exception {
        mvc.perform(get("/historical")
                .param("from", "EUR").param("to", "USD")
                .param("fromDate", "2024-03-07").param("toDate", "2024-03-01"))
           .andExpect(status().isBadRequest());
    }

    @Test
    void historical_returns404WhenNoRates() throws Exception {
        when(exchangeRateService.historicalAdjusted(any(), any(), any(), any()))
                .thenThrow(new RateNotFoundException("No rates"));

        mvc.perform(get("/historical")
                .param("from", "EUR").param("to", "USD")
                .param("fromDate", "2024-03-01").param("toDate", "2024-03-07"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.title").value("Exchange rate not found"));
    }
}
