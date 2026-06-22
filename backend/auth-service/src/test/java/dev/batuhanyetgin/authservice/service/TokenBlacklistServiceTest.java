package dev.batuhanyetgin.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private TokenBlacklistService tokenBlacklistService;

    private static final String TOKEN = "some.jwt.token";
    private static final String KEY   = "blacklist:" + TOKEN;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        tokenBlacklistService = new TokenBlacklistService(redisTemplate);
    }

    // ─── blacklist ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("blacklist")
    class Blacklist {

        @Test
        @DisplayName("positive ttl → stores token in Redis with given TTL")
        void positiveTtl_storesInRedis() {
            tokenBlacklistService.blacklist(TOKEN, 3600L);

            verify(valueOps).set(KEY, "1", 3600L, TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("zero ttl → does not store token")
        void zeroTtl_doesNotStore() {
            tokenBlacklistService.blacklist(TOKEN, 0L);

            verify(valueOps, never()).set(any(), any(), anyLong(), any());
        }

        @Test
        @DisplayName("negative ttl → does not store token")
        void negativeTtl_doesNotStore() {
            tokenBlacklistService.blacklist(TOKEN, -1L);

            verify(valueOps, never()).set(any(), any(), anyLong(), any());
        }
    }

    // ─── isBlacklisted ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isBlacklisted")
    class IsBlacklisted {

        @Test
        @DisplayName("key present in Redis → returns true")
        void keyPresent_returnsTrue() {
            when(redisTemplate.hasKey(KEY)).thenReturn(Boolean.TRUE);

            assertThat(tokenBlacklistService.isBlacklisted(TOKEN)).isTrue();
        }

        @Test
        @DisplayName("key absent in Redis → returns false")
        void keyAbsent_returnsFalse() {
            when(redisTemplate.hasKey(KEY)).thenReturn(Boolean.FALSE);

            assertThat(tokenBlacklistService.isBlacklisted(TOKEN)).isFalse();
        }

        @Test
        @DisplayName("Redis returns null → returns false")
        void nullResponse_returnsFalse() {
            when(redisTemplate.hasKey(KEY)).thenReturn(null);

            assertThat(tokenBlacklistService.isBlacklisted(TOKEN)).isFalse();
        }
    }
}
