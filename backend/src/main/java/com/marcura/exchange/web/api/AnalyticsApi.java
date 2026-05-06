package com.marcura.exchange.web.api;

import com.marcura.exchange.web.dto.AnalyticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Analytics", description = "Currency usage analytics")
@RequestMapping("/analytics")
public interface AnalyticsApi {

    @Operation(summary = "Top currencies by total query count")
    @GetMapping
    AnalyticsResponse analytics();
}
