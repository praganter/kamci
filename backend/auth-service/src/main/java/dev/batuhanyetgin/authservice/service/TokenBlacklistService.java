package dev.batuhanyetgin.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(final String token, final long ttlSeconds) {
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(PREFIX + token, "1", ttlSeconds, TimeUnit.SECONDS);
        }
    }

    public boolean isBlacklisted(final String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}
