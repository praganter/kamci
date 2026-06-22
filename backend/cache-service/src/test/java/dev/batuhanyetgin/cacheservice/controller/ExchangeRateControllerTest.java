package dev.batuhanyetgin.cacheservice.controller;

import dev.batuhanyetgin.cacheservice.dto.ExchangeRateResponse;
import dev.batuhanyetgin.cacheservice.service.ExchangeRateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateControllerTest {

    @Mock private ExchangeRateService exchangeRateService;

    @InjectMocks private ExchangeRateController controller;

    private static final ExchangeRateResponse RESPONSE =
            new ExchangeRateResponse("2026-01-01", "USD", Map.of("eur", 0.9, "try", 32.5));

    // ─── getRates ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRates")
    class GetRates {

        @Test
        @DisplayName("delegates to service and returns 200 with response body")
        void delegatesToServiceAndReturns200() {
            when(exchangeRateService.getRates("USD")).thenReturn(RESPONSE);

            final var result = controller.getRates("USD");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo(RESPONSE);
            verify(exchangeRateService).getRates("USD");
        }
    }

    // ─── getRate ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRate")
    class GetRate {

        @Test
        @DisplayName("delegates to service and returns 200 with rate map")
        void delegatesToServiceAndReturns200() {
            when(exchangeRateService.getRate("USD", "EUR")).thenReturn(0.9);

            final var result = controller.getRate("USD", "EUR");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).containsEntry("base", "USD");
            assertThat(result.getBody()).containsEntry("target", "EUR");
            assertThat(result.getBody()).containsEntry("rate", 0.9);
            verify(exchangeRateService).getRate("USD", "EUR");
        }

        @Test
        @DisplayName("base and target uppercased in response map")
        void baseAndTargetUppercasedInResponse() {
            when(exchangeRateService.getRate("usd", "eur")).thenReturn(0.9);

            final var result = controller.getRate("usd", "eur");

            assertThat(result.getBody()).containsEntry("base", "USD");
            assertThat(result.getBody()).containsEntry("target", "EUR");
        }
    }
}
