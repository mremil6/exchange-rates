package com.marcura.exchange.web.api;

import com.marcura.exchange.web.dto.HistoricalRatesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Tag(name = "Historical", description = "Historical spread-adjusted rates for a currency pair")
@RequestMapping("/historical")
public interface HistoricalApi {

    @Operation(summary = "Daily spread-adjusted rates for the given pair across a date range")
    @GetMapping
    HistoricalRatesResponse historical(
            @RequestParam @NotBlank String from,
            @RequestParam @NotBlank String to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    );
}
