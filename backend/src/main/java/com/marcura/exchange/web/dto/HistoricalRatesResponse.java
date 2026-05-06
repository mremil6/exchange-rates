package com.marcura.exchange.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HistoricalRatesResponse(
        String from,
        String to,
        LocalDate fromDate,
        LocalDate toDate,
        List<Point> points
) {
    public record Point(LocalDate date, BigDecimal rate) {}
}
