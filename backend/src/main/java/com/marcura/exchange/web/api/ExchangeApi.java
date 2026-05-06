package com.marcura.exchange.web.api;

import com.marcura.exchange.web.dto.ExchangeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Tag(name = "Exchange", description = "Spread-adjusted exchange rates")
@RequestMapping("/exchange")
public interface ExchangeApi {

    @Operation(
        summary = "Get the spread-adjusted exchange rate for a currency pair",
        description = "Optionally pass a `date`; otherwise the latest available rates are used. Increments usage counters for both currencies."
    )
    @ApiResponse(responseCode = "200", description = "Rate found")
    @ApiResponse(responseCode = "404", description = "No rate available for the requested date")
    @ApiResponse(responseCode = "400", description = "Invalid currency code or date")
    @GetMapping
    ExchangeResponse exchange(
            @RequestParam @NotBlank String from,
            @RequestParam @NotBlank String to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );
}
