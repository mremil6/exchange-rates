package com.marcura.exchange.web.dto;

import java.time.LocalDate;

public record InsightResponse(
        String from,
        String to,
        LocalDate fromDate,
        LocalDate toDate,
        String insight
) {}
