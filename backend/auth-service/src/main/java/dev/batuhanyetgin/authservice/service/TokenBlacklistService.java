package dev.batuhanyetgin.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(final String token, final long ttlSeconds) {
        if (ttlSeconds > 0) {
            try {
                redisTemplate.opsForValue().set(PREFIX + token, "1", ttlSeconds, TimeUnit.SECONDS);
            } catch (DataAccessException e) {
                log.warn("Redis unavailable, token not blacklisted: {}", e.getMessage());
            }
        }
    }

    public boolean isBlacklisted(final String token) {
        // Fail open: a Redis outage must not turn every authenticated call into a 500.
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
        } catch (DataAccessException e) {
            log.warn("Redis unavailable, treating token as not blacklisted: {}", e.getMessage());
            return false;
        }
    }
}
