package dev.batuhanyetgin.gatewayserver;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Test-only token factory; uses the same HMAC secret the tests inject as {@code JWT_SECRET}. */
public final class GatewayTestSupport {

    public static final String SECRET = "test-secret-test-secret-test-secret-0123456789";

    private GatewayTestSupport() {
    }

    public static String token(String subject, String type, boolean premium, long ttlMillis, String secret) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .claim("type", type)
                .claim("premium", premium)
                .issuedAt(new Date(now - 10_000))
                .expiration(new Date(now + ttlMillis))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public static String accessToken(String subject, boolean premium) {
        return token(subject, "ACCESS", premium, 60_000, SECRET);
    }
}
