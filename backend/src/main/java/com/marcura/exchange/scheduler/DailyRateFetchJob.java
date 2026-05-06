package com.marcura.exchange.scheduler;

import com.marcura.exchange.service.ingestion.RateIngestionService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers the daily rate fetch at 12:05 AM GMT.
 * {@code @SchedulerLock} ensures only one instance runs per day; {@code lockAtMostFor}
 * force-releases a stale lock from a crashed node so the next run is never blocked.
 */
@Component
@RequiredArgsConstructor
public class DailyRateFetchJob {

    private static final Logger log = LoggerFactory.getLogger(DailyRateFetchJob.class);

    private final RateIngestionService ingestionService;

    @Scheduled(cron = "0 5 0 * * *", zone = "GMT")
    @SchedulerLock(name = "dailyRateFetch", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void fetchDaily() {
        log.info("Daily rate fetch triggered");
        int written = ingestionService.ingestLatest();
        log.info("Daily rate fetch complete; rows written/refreshed = {}", written);
    }
}
