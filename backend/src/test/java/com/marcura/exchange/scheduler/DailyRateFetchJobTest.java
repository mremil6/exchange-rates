package com.marcura.exchange.scheduler;

import com.marcura.exchange.service.ingestion.RateIngestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyRateFetchJobTest {

    @Mock RateIngestionService ingestionService;
    @InjectMocks DailyRateFetchJob job;

    @Test
    void fetchDaily_delegatesToIngestionService() {
        when(ingestionService.ingestLatest()).thenReturn(18);

        job.fetchDaily();

        verify(ingestionService).ingestLatest();
    }
}
