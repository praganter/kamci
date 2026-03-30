package dev.batuhanyetgin.authservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    // Must be >= 32 bytes for HS256
    private static final String SECRET = "test-secret-key-minimum-32-bytes-long!!";
    private static final long EXPIRATION_MS = 3_600_000L;        // 1 hour
    private static final long REFRESH_EXPIRATION_MS = 86_400_000L; // 24 hours
    private static final String USER_ID = "user123";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationRefreshMs", REFRESH_EXPIRATION_MS);
    }

    // ─── generateToken ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("access token contains type=ACCESS and correct subject")
        void accessToken_containsCorrectClaims() {
            final String token = jwtService.generateToken(USER_ID, false, false);

            final Claims claims = jwtService.validateToken(token);
            assertThat(claims.getSubject()).isEqualTo(USER_ID);
            assertThat(claims.get("type", String.class)).isEqualTo("ACCESS");
        }

        @Test
        @DisplayName("refresh token contains type=REFRESH and correct subject")
        void refreshToken_containsCorrectClaims() {
            final String token = jwtService.generateToken(USER_ID, true, false);

            final Claims claims = jwtService.validateToken(token);
            assertThat(claims.getSubject()).isEqualTo(USER_ID);
            assertThat(claims.get("type", String.class)).isEqualTo("REFRESH");
        }

        @Test
        @DisplayName("null userId throws NullPointerException")
        void nullUserId_throws() {
            assertThatNullPointerException()
                    .isThrownBy(() -> jwtService.generateToken(null, false, false));
        }
    }

    // ─── validateToken ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("valid token returns claims")
        void validToken_returnsClaims() {
            final String token = jwtService.generateToken(USER_ID, false, false);
            assertThat(jwtService.validateToken(token)).isNotNull();
        }

        @Test
        @DisplayName("blank token throws IllegalArgumentException")
        void blankToken_throwsIllegalArgument() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> jwtService.validateToken("   "));
        }

        @Test
        @DisplayName("empty token throws IllegalArgumentException")
        void emptyToken_throwsIllegalArgument() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> jwtService.validateToken(""));
        }

        @Test
        @DisplayName("tampered token throws JwtException")
        void tamperedToken_throwsJwtException() {
            final String token = jwtService.generateToken(USER_ID, false, false);
            final String tampered = token.substring(0, token.length() - 5) + "XXXXX";

            assertThatExceptionOfType(JwtException.class)
                    .isThrownBy(() -> jwtService.validateToken(tampered));
        }

        @Test
        @DisplayName("expired token throws JwtException")
        void expiredToken_throwsJwtException() {
            ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -1L);
            final String token = jwtService.generateToken(USER_ID, false, false);

            // restore
            ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", EXPIRATION_MS);

            assertThatExceptionOfType(JwtException.class)
                    .isThrownBy(() -> jwtService.validateToken(token));
        }
    }

    // ─── getUserIdFromToken ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserIdFromToken")
    class GetUserIdFromToken {

        @Test
        @DisplayName("returns userId from valid token")
        void validToken_returnsUserId() {
            final String token = jwtService.generateToken(USER_ID, false, false);
            assertThat(jwtService.getUserIdFromToken(token)).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("null token throws NullPointerException")
        void nullToken_throws() {
            assertThatNullPointerException()
                    .isThrownBy(() -> jwtService.getUserIdFromToken(null));
        }
    }

    // ─── validateRefreshToken ─────────────────────────────────────────────────

    @Nested
    @DisplayName("validateRefreshToken")
    class ValidateRefreshToken {

        @Test
        @DisplayName("valid refresh token returns userId")
        void validRefreshToken_returnsUserId() {
            final String token = jwtService.generateToken(USER_ID, true, false);
            assertThat(jwtService.validateRefreshToken(token)).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("access token passed as refresh throws JwtException")
        void accessTokenAsRefresh_throwsJwtException() {
            final String accessToken = jwtService.generateToken(USER_ID, false, false);

            assertThatExceptionOfType(JwtException.class)
                    .isThrownBy(() -> jwtService.validateRefreshToken(accessToken))
                    .withMessageContaining("Invalid token type");
        }
    }

    // ─── generatePasswordResetToken / validatePasswordResetToken ─────────────

    @Nested
    @DisplayName("password reset token")
    class PasswordResetToken {

        @Test
        @DisplayName("generated token validates successfully and returns userId")
        void generateAndValidate_returnsUserId() {
            final String token = jwtService.generatePasswordResetToken(USER_ID);
            assertThat(jwtService.validatePasswordResetToken(token)).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("access token passed as reset throws JwtException")
        void accessTokenAsReset_throwsJwtException() {
            final String accessToken = jwtService.generateToken(USER_ID, false, false);

            assertThatExceptionOfType(JwtException.class)
                    .isThrownBy(() -> jwtService.validatePasswordResetToken(accessToken))
                    .withMessageContaining("Invalid token type");
        }

        @Test
        @DisplayName("null userId throws NullPointerException")
        void nullUserId_throws() {
            assertThatNullPointerException()
                    .isThrownBy(() -> jwtService.generatePasswordResetToken(null));
        }
    }
}
