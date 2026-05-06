package com.marcura.exchange.web;

import com.marcura.exchange.service.AnalyticsService;
import com.marcura.exchange.web.api.AnalyticsApi;
import com.marcura.exchange.web.dto.AnalyticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
public class AnalyticsController implements AnalyticsApi {

    private final AnalyticsService service;

    @Override
    public AnalyticsResponse analytics() {
        AnalyticsService.Snapshot s = service.snapshot();
        return new AnalyticsResponse(
                s.top().stream()
                        .map(u -> new AnalyticsResponse.TopCurrency(
                                u.getCurrency(), u.getTotalCount(), u.getLastQueriedAt()))
                        .toList()
        );
    }
}
