package dev.batuhanyetgin.cacheservice.scheduler;

import dev.batuhanyetgin.cacheservice.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    private final ExchangeRateService exchangeRateService;

    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(
            name = "exchangeRateScheduler",
            lockAtMostFor = "PT55M",
            lockAtLeastFor = "PT50M"
    )
    public void refreshRates() {
        log.info("Starting hourly exchange rate refresh");
        exchangeRateService.fetchAndCacheAll();
        log.info("Exchange rate refresh complete");
    }
}
