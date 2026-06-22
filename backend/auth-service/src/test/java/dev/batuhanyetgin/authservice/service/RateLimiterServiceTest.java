package dev.batuhanyetgin.authservice.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private RateLimiterService rateLimiterService;

    private static final String KEY        = "login:user@example.com";
    private static final String REDIS_KEY  = "rate:" + KEY;
    private static final String ERROR_KEY  = "error.auth.too_many_login_attempts";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        rateLimiterService = new RateLimiterService(redisTemplate);
    }

    // ─── check ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("check")
    class Check {

        @Test
        @DisplayName("first attempt (count=1) → sets TTL window and does not throw")
        void firstAttempt_setsTtlAndNoException() {
            when(valueOps.increment(REDIS_KEY)).thenReturn(1L);

            assertThatNoException().isThrownBy(() ->
                    rateLimiterService.check(KEY, 5, 900L, ERROR_KEY));

            verify(redisTemplate).expire(REDIS_KEY, 900L, TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("within limit (count=3, max=5) → no TTL set, no exception")
        void withinLimit_noTtlAndNoException() {
            when(valueOps.increment(REDIS_KEY)).thenReturn(3L);

            assertThatNoException().isThrownBy(() ->
                    rateLimiterService.check(KEY, 5, 900L, ERROR_KEY));

            verify(redisTemplate, never()).expire(any(), anyLong(), any());
        }

        @Test
        @DisplayName("exactly at limit (count=5, max=5) → no exception")
        void atLimit_noException() {
            when(valueOps.increment(REDIS_KEY)).thenReturn(5L);

            assertThatNoException().isThrownBy(() ->
                    rateLimiterService.check(KEY, 5, 900L, ERROR_KEY));
        }

        @Test
        @DisplayName("exceeds limit (count=6, max=5) → throws TOO_MANY_REQUESTS")
        void exceedsLimit_throwsTooManyRequests() {
            when(valueOps.increment(REDIS_KEY)).thenReturn(6L);

            final Throwable t = catchThrowable(() ->
                    rateLimiterService.check(KEY, 5, 900L, ERROR_KEY));

            assertThat(t).isInstanceOf(BaseException.class);
            assertThat(((BaseException) t).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("Redis returns null count → no exception, no TTL set")
        void nullCount_noExceptionAndNoTtl() {
            when(valueOps.increment(REDIS_KEY)).thenReturn(null);

            assertThatNoException().isThrownBy(() ->
                    rateLimiterService.check(KEY, 5, 900L, ERROR_KEY));

            verify(redisTemplate, never()).expire(any(), anyLong(), any());
        }
    }

    // ─── reset ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reset")
    class Reset {

        @Test
        @DisplayName("reset → deletes rate key from Redis")
        void reset_deletesKey() {
            rateLimiterService.reset(KEY);

            verify(redisTemplate).delete(REDIS_KEY);
        }
    }
}
