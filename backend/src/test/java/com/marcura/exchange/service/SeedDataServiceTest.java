package com.marcura.exchange.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcura.exchange.repository.ExchangeRateRepository;
import com.marcura.exchange.service.ingestion.SeedDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeedDataServiceTest {

    private ExchangeRateRepository repo;
    private SeedDataService service;

    @BeforeEach
    void setUp() {
        repo = mock(ExchangeRateRepository.class);
        service = new SeedDataService(repo, new ObjectMapper());
        ReflectionTestUtils.setField(service, "seedDays", 3);
    }

    @Test
    void seedIfEmpty_returnsZeroWhenAlreadySeeded() {
        when(repo.count()).thenReturn(100L);

        assertThat(service.seedIfEmpty()).isEqualTo(0);
        verify(repo, never()).saveAll(any());
    }

    @Test
    void seedIfEmpty_generatesRowsWhenEmpty() {
        when(repo.count()).thenReturn(0L);

        int result = service.seedIfEmpty();

        assertThat(result).isGreaterThan(0);
        verify(repo).saveAll(any());
    }

    @Test
    void onReady_skipsWhenRatesExist() {
        when(repo.count()).thenReturn(50L);

        service.onReady();

        verify(repo, never()).saveAll(any());
    }

    @Test
    void onReady_seedsWhenEmpty() {
        when(repo.count()).thenReturn(0L);

        service.onReady();

        verify(repo).saveAll(any());
    }
}
