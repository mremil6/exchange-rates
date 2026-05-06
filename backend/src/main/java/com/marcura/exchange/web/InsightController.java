package com.marcura.exchange.web;

import com.marcura.exchange.service.ExchangeRateService;
import com.marcura.exchange.service.insight.TrendInsightService;
import com.marcura.exchange.web.api.InsightApi;
import com.marcura.exchange.web.dto.HistoricalRatesResponse;
import com.marcura.exchange.web.dto.InsightResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Validated
public class InsightController implements InsightApi {

    private final ExchangeRateService exchangeRateService;
    private final TrendInsightService insightService;

    @Override
    public InsightResponse insight(String from, String to, LocalDate fromDate, LocalDate toDate) {
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate must not be before fromDate");
        }
        HistoricalRatesResponse series = exchangeRateService.historicalAdjusted(from, to, fromDate, toDate);
        String text = insightService.generate(series);
        return new InsightResponse(series.from(), series.to(), series.fromDate(), series.toDate(), text);
    }
}
