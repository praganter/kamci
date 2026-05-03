package dev.batuhanyetgin.cacheservice.controller;

import dev.batuhanyetgin.cacheservice.dto.ExchangeRateResponse;
import dev.batuhanyetgin.cacheservice.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/{base}")
    public ResponseEntity<ExchangeRateResponse> getRates(@PathVariable final String base) {
        return ResponseEntity.ok(exchangeRateService.getRates(base));
    }

    @GetMapping("/{base}/{target}")
    public ResponseEntity<Map<String, Object>> getRate(
            @PathVariable final String base,
            @PathVariable final String target) {
        final double rate = exchangeRateService.getRate(base, target);
        return ResponseEntity.ok(Map.of(
                "base", base.toUpperCase(),
                "target", target.toUpperCase(),
                "rate", rate
        ));
    }
}
