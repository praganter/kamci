package dev.batuhanyetgin.kamci.client;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExchangeRateClientFallbackFactory implements FallbackFactory<ExchangeRateClient> {

    @Override
    public ExchangeRateClient create(final Throwable cause) {
        return (base, target) -> {
            log.error("cache-service unavailable for getRate(base={}, target={}): {}", base, target, cause.getMessage());
            throw BaseException.serviceUnavailable("error.cache_service.unavailable");
        };
    }
}
