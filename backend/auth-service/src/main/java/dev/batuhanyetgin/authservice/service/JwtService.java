package dev.batuhanyetgin.authservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.expirationRefresh}")
    private long jwtExpirationRefreshMs;

    public Claims validateToken(final String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Invalid JWT token: " + e.getMessage(), e);
        }
    }

    public String getUserIdFromToken(final String token) {
        Objects.requireNonNull(token, "Token cannot be null");
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    public String generateToken(final String userId, final boolean isRefreshToken) {
        Objects.requireNonNull(userId, "User ID cannot be null");

        Map<String, Object> claims = new HashMap<>();
        final Date tokenLife = isRefreshToken
                ? new Date(System.currentTimeMillis() + jwtExpirationRefreshMs)
                : new Date(System.currentTimeMillis() + jwtExpirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuedAt(new Date())
                .expiration(tokenLife)
                .signWith(getSigningKey())
                .compact();
    }

    public String generatePasswordResetToken(final String userId) {
        Objects.requireNonNull(userId, "User ID cannot be null");

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "PASSWORD_RESET");

        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000L))
                .signWith(getSigningKey())
                .compact();
    }

    public String validatePasswordResetToken(final String token) {
        final Claims claims = validateToken(token);
        final String type = claims.get("type", String.class);

        if (!"PASSWORD_RESET".equals(type)) {
            throw new JwtException("Invalid token type");
        }

        return claims.getSubject();
    }

    private SecretKey getSigningKey() {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException("JWT secret is not configured");
        }

        final byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}