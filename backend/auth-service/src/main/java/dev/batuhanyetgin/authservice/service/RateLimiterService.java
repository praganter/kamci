package dev.batuhanyetgin.authservice.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private static final String PREFIX = "rate:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Increments the counter for the given key and throws TOO_MANY_REQUESTS
     * if the limit is exceeded. The TTL is set only on the first increment
     * so the window always starts from the first attempt.
     */
    public void check(final String key, final int maxAttempts, final long windowSeconds, final String errorMessageKey) {
        final String redisKey = PREFIX + key;
        final Long count;
        try {
            count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
            }
        } catch (DataAccessException e) {
            // Fail open: rate limiting is best-effort, auth must keep working without Redis.
            log.warn("Redis unavailable, skipping rate limit for key {}: {}", key, e.getMessage());
            return;
        }

        if (count != null && count > maxAttempts) {
            throw BaseException.tooManyRequests(errorMessageKey);
        }
    }

    public void reset(final String key) {
        try {
            redisTemplate.delete(PREFIX + key);
        } catch (DataAccessException e) {
            log.warn("Redis unavailable, rate limit key {} not reset: {}", key, e.getMessage());
        }
    }
}
