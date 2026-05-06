package com.marcura.exchange.service;

import com.marcura.exchange.domain.CurrencyUsage;
import com.marcura.exchange.repository.CurrencyUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final CurrencyUsageRepository totalRepo;

    public record Snapshot(List<CurrencyUsage> top) {}

    @Transactional(readOnly = true)
    public Snapshot snapshot() {
        return new Snapshot(totalRepo.findAllOrderByCountDesc());
    }
}
