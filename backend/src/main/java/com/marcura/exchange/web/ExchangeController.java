package com.marcura.exchange.web;

import com.marcura.exchange.service.ExchangeRateService;
import com.marcura.exchange.web.api.ExchangeApi;
import com.marcura.exchange.web.dto.ExchangeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Validated
public class ExchangeController implements ExchangeApi {

    private final ExchangeRateService service;

    @Override
    public ExchangeResponse exchange(String from, String to, LocalDate date) {
        ExchangeRateService.Result r = service.exchange(from, to, Optional.ofNullable(date));
        return new ExchangeResponse(r.from(), r.to(), r.exchange(), r.date(),
                r.fromQueryCount(), r.toQueryCount());
    }
}
