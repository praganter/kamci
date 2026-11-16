package dev.batuhanyetgin.cacheservice.service;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import dev.batuhanyetgin.cacheservice.dto.ExchangeRateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock private RestClient.Builder restClientBuilder;
    @Mock private RestClient restClient;
    @SuppressWarnings("rawtypes")
    @Mock private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @SuppressWarnings("rawtypes")
    @Mock private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private RestClient.ResponseSpec responseSpec;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private ExchangeRateService service;

    private static final String BASE  = "USD";
    private static final String KEY   = "exchange_rates:USD";

    // Minimal API JSON response as returned by the exchange-rate provider
    private static final String API_JSON =
            "{\"date\":\"2026-01-01\",\"usd\":{\"eur\":0.9,\"try\":32.5}}";

    // Pre-serialized ExchangeRateResponse (the format stored in Redis)
    private static final String CACHED_JSON =
            "{\"date\":\"2026-01-01\",\"base\":\"USD\",\"rates\":{\"eur\":0.9,\"try\":32.5}}";

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(restClientBuilder.build()).thenReturn(restClient);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new ExchangeRateService(restClientBuilder, redisTemplate, objectMapper);
        ReflectionTestUtils.setField(service, "apiUrl", "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies");
        ReflectionTestUtils.setField(service, "baseCurrencies", new String[]{"USD", "EUR"});
    }

    // ─── getRates ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRates")
    class GetRates {

        @Test
        @DisplayName("cache hit → returns deserialized response without calling API")
        void cacheHit_returnsCachedResponse() {
            when(valueOps.get(KEY)).thenReturn(CACHED_JSON);

            final ExchangeRateResponse result = service.getRates(BASE);

            assertThat(result.base()).isEqualTo("USD");
            assertThat(result.rates()).containsEntry("eur", 0.9);
            verify(restClient, never()).get();
        }

        @Test
        @DisplayName("cache miss → fetches from API and returns response")
        @SuppressWarnings("unchecked")
        void cacheMiss_fetchesAndReturns() {
            when(valueOps.get(KEY))
                    .thenReturn(null)        // first call → miss
                    .thenReturn(CACHED_JSON); // second call → after fetch

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(String.class)).thenReturn(API_JSON);

            final ExchangeRateResponse result = service.getRates(BASE);

            assertThat(result.base()).isEqualTo("USD");
            verify(valueOps).set(eq(KEY), anyString(), any());
        }

        @Test
        @DisplayName("cache miss and API fetch produces nothing → throws IllegalStateException")
        @SuppressWarnings("unchecked")
        void cacheMissAndFetchProducesNothing_throwsIllegalState() {
            // Both Redis reads return null (fetch writes nothing due to empty API body)
            when(valueOps.get(KEY)).thenReturn(null);

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(String.class)).thenReturn(null); // empty API response

            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> service.getRates(BASE))
                    .withMessageContaining("unavailable");
        }
    }

    // ─── getRate ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRate")
    class GetRate {

        @Test
        @DisplayName("target currency found → returns correct rate")
        void targetFound_returnsRate() {
            when(valueOps.get(KEY)).thenReturn(CACHED_JSON);

            final double rate = service.getRate(BASE, "EUR");

            assertThat(rate).isEqualTo(0.9);
        }

        @Test
        @DisplayName("target currency not in rates → throws IllegalArgumentException")
        void targetNotFound_throwsIllegalArgument() {
            when(valueOps.get(KEY)).thenReturn(CACHED_JSON);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> service.getRate(BASE, "JPY"))
                    .withMessageContaining("USD/JPY");
        }
    }

    // ─── fetchAndCacheAll ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("fetchAndCacheAll")
    class FetchAndCacheAll {

        @Test
        @DisplayName("fetches and caches each configured base currency")
        @SuppressWarnings("unchecked")
        void fetchesEachBaseCurrency() {
            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            // Return valid JSON for USD, EUR
            when(responseSpec.body(String.class))
                    .thenReturn("{\"date\":\"2026-01-01\",\"usd\":{\"eur\":0.9}}")
                    .thenReturn("{\"date\":\"2026-01-01\",\"eur\":{\"usd\":1.11}}");

            service.fetchAndCacheAll();

            // One set call per base currency
            verify(valueOps).set(eq("exchange_rates:USD"), anyString(), any());
            verify(valueOps).set(eq("exchange_rates:EUR"), anyString(), any());
        }

        @Test
        @DisplayName("API failure for one currency → swallowed, other currencies still processed")
        @SuppressWarnings("unchecked")
        void apiFailureForOne_othersStillProcessed() {
            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(String.class))
                    .thenThrow(new RuntimeException("network error"))
                    .thenReturn("{\"date\":\"2026-01-01\",\"eur\":{\"usd\":1.11}}");

            assertThatNoException().isThrownBy(() -> service.fetchAndCacheAll());

            verify(valueOps, times(1)).set(eq("exchange_rates:EUR"), anyString(), any());
        }
    }
}
