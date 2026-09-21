package dev.batuhanyetgin.kamci.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cache-service", fallbackFactory = ExchangeRateClientFallbackFactory.class)
public interface ExchangeRateClient {

    @GetMapping("rates/{base}/{target}")
    ExchangeRateDto getRate(@PathVariable String base, @PathVariable String target);
}
