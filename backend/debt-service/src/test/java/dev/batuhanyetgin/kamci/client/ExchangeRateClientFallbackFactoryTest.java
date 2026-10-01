package dev.batuhanyetgin.kamci.client;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateClientFallbackFactoryTest {

    private final ExchangeRateClientFallbackFactory factory = new ExchangeRateClientFallbackFactory();

    @Test
    @DisplayName("getRate → throws service-unavailable")
    void getRate_throwsServiceUnavailable() {
        final ExchangeRateClient fallback = factory.create(new RuntimeException("cache-service down"));

        assertThatThrownBy(() -> fallback.getRate("USD", "EUR"))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
