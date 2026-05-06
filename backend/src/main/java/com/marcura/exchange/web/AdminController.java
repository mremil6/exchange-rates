package com.marcura.exchange.web;

import com.marcura.exchange.service.ingestion.RateIngestionService;
import com.marcura.exchange.web.api.AdminApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminController implements AdminApi {

    private final RateIngestionService ingestionService;

    @Override
    public Map<String, Object> refresh() {
        int rows = ingestionService.ingestLatest();
        return Map.of("rowsWritten", rows);
    }
}
