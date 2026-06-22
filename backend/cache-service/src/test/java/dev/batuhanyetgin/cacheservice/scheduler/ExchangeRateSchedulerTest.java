package dev.batuhanyetgin.cacheservice.scheduler;

import dev.batuhanyetgin.cacheservice.service.ExchangeRateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateSchedulerTest {

    @Mock private ExchangeRateService exchangeRateService;

    @InjectMocks private ExchangeRateScheduler scheduler;

    @Test
    @DisplayName("refreshRates → delegates to exchangeRateService.fetchAndCacheAll")
    void refreshRates_delegatesToService() {
        scheduler.refreshRates();

        verify(exchangeRateService).fetchAndCacheAll();
    }
}
