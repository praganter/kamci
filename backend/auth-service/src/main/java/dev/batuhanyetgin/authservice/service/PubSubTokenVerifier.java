package dev.batuhanyetgin.authservice.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.security.PublicKey;
import java.util.Set;

/**
 * Authenticates a Google Pub/Sub push request: {@code Authorization: Bearer <OIDC JWT>} signed by
 * Google (RS256, key from Google's JWKS) for the configured audience and service account.
 * Anything else, including a missing configuration, is a 403.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PubSubTokenVerifier {

    private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    private static final String BEARER = "Bearer ";

    private final IapSettings settings;
    private final PubSubKeyProvider keyProvider;

    public void verify(final String authorizationHeader) {
        if (!settings.isPubSubConfigured()) {
            log.warn("Google Pub/Sub push authentication is not configured, rejecting the notification");
            throw forbidden();
        }
        if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            throw forbidden();
        }

        final Claims claims;
        try {
            claims = Jwts.parser()
                    .keyLocator(new LocatorAdapter<Key>() {
                        @Override
                        protected Key locate(final JwsHeader header) {
                            if (!"RS256".equals(header.getAlgorithm())) {
                                throw new JwtException("Unexpected algorithm: " + header.getAlgorithm());
                            }
                            final PublicKey key = keyProvider.keyFor(header.getKeyId());
                            if (key == null) {
                                throw new JwtException("Unknown signing key");
                            }
                            return key;
                        }
                    })
                    .requireAudience(settings.getPubSubAudience())
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(authorizationHeader.substring(BEARER.length()).trim())
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Rejected Pub/Sub push token: {}", e.getMessage());
            throw forbidden();
        }

        final String email = claims.get("email", String.class);
        if (!ISSUERS.contains(claims.getIssuer())
                || email == null || !email.equalsIgnoreCase(settings.getPubSubServiceAccountEmail())
                || !isTrue(claims.get("email_verified"))) {
            log.warn("Rejected Pub/Sub push token: issuer, email or email_verified does not match");
            throw forbidden();
        }
    }

    private static boolean isTrue(final Object value) {
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }

    private static BaseException forbidden() {
        return BaseException.forbidden("error.subscription.webhook_unauthorized");
    }
}
