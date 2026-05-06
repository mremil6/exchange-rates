package com.marcura.exchange.web;

import com.marcura.exchange.service.ExchangeRateService;
import com.marcura.exchange.web.api.HistoricalApi;
import com.marcura.exchange.web.dto.HistoricalRatesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Validated
public class HistoricalController implements HistoricalApi {

    private final ExchangeRateService exchangeRateService;

    @Override
    public HistoricalRatesResponse historical(String from, String to, LocalDate fromDate, LocalDate toDate) {
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate must not be before fromDate");
        }
        return exchangeRateService.historicalAdjusted(from, to, fromDate, toDate);
    }
}
