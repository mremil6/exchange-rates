package com.marcura.exchange.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeResponse(
        String from,
        String to,
        BigDecimal exchange,
        LocalDate date,
        long fromQueryCount,
        long toQueryCount
) {}
