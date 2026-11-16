package dev.batuhanyetgin.cacheservice.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import dev.batuhanyetgin.cacheservice.dto.ExchangeRateResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExchangeRateService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${exchange-rate.api-url}")
    private String apiUrl;

    @Value("${exchange-rate.base-currencies}")
    private String[] baseCurrencies;

    private static final Duration CACHE_TTL = Duration.ofHours(25);
    private static final String KEY_PREFIX = "exchange_rates:";

    public ExchangeRateService(final RestClient.Builder restClientBuilder,
                               final StringRedisTemplate redisTemplate,
                               final ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            fetchAndCacheAll();
        } catch (Exception e) {
            log.error("Initial exchange rate load failed — rates will be fetched on first request", e);
        }
    }

    public void fetchAndCacheAll() {
        for (final String base : baseCurrencies) {
            try {
                fetchAndCache(base.toLowerCase());
            } catch (Exception e) {
                log.error("Failed to fetch/cache rates for base currency: {}", base, e);
            }
        }
    }

    public ExchangeRateResponse getRates(final String base) {
        final String key = KEY_PREFIX + base.toUpperCase();
        final String cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            return deserialize(cached);
        }

        log.info("Cache miss for {}, fetching from API", base.toUpperCase());
        fetchAndCache(base.toLowerCase());

        final String fresh = redisTemplate.opsForValue().get(key);
        if (fresh == null) {
            throw new IllegalStateException("Exchange rates unavailable for: " + base.toUpperCase());
        }
        return deserialize(fresh);
    }

    public double getRate(final String base, final String target) {
        final ExchangeRateResponse rates = getRates(base);
        final Double rate = rates.rates().get(target.toLowerCase());
        if (rate == null) {
            throw new IllegalArgumentException("No rate found for pair: " + base.toUpperCase() + "/" + target.toUpperCase());
        }
        return rate;
    }

    @SuppressWarnings("unchecked")
    private void fetchAndCache(final String base) {
        final String url = apiUrl + "/" + base + ".json";
        log.info("Fetching exchange rates for base: {}", base.toUpperCase());

        final String responseJson = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        if (responseJson == null) {
            log.warn("Empty response from exchange API for base: {}", base);
            return;
        }

        final Map<String, Object> response;
        try {
            response = objectMapper.readValue(responseJson, Map.class);
        } catch (JacksonException e) {
            log.error("Failed to parse exchange API response for base: {}", base, e);
            return;
        }

        final String date = (String) response.get("date");
        final Map<String, Object> ratesRaw = (Map<String, Object>) response.get(base);
        if (ratesRaw == null) {
            log.warn("No rates object in response for base: {}", base);
            return;
        }

        final Map<String, Double> rates = ratesRaw.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> ((Number) e.getValue()).doubleValue()));

        final ExchangeRateResponse exchangeRate = new ExchangeRateResponse(date, base.toUpperCase(), rates);
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + base.toUpperCase(), objectMapper.writeValueAsString(exchangeRate), CACHE_TTL);
            log.info("Cached {} rates for {} (date: {})", rates.size(), base.toUpperCase(), date);
        } catch (JacksonException e) {
            log.error("Failed to serialize exchange rates for base: {}", base, e);
        }
    }

    private ExchangeRateResponse deserialize(final String json) {
        try {
            return objectMapper.readValue(json, ExchangeRateResponse.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to deserialize cached exchange rates", e);
        }
    }
}
