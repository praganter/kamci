package dev.batuhanyetgin.authservice.service;

import dev.batuhanyetgin.authservice.client.DebtServiceClient;
import dev.batuhanyetgin.authservice.client.NotificationServiceClient;
import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.document.HouseholdDocument;
import dev.batuhanyetgin.authservice.repositoryservice.HouseholdRepositoryService;
import dev.batuhanyetgin.authservice.dto.request.ChangePasswordRequest;
import dev.batuhanyetgin.authservice.dto.request.ForgetPasswordRequest;
import dev.batuhanyetgin.authservice.dto.request.LoginRequest;
import dev.batuhanyetgin.authservice.dto.request.RegisterRequest;
import dev.batuhanyetgin.authservice.dto.request.ResetPasswordRequest;
import dev.batuhanyetgin.authservice.dto.response.TokenResponse;
import dev.batuhanyetgin.authservice.dto.response.UserDetailResponse;
import dev.batuhanyetgin.authservice.producer.AuthEventProducer;
import dev.batuhanyetgin.authservice.repositoryservice.AuthRepositoryService;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private JwtService jwtService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private AuthRepositoryService authRS;
    @Mock private AuthEventProducer authEventProducer;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private RateLimiterService rateLimiterService;
    @Mock private HouseholdRepositoryService householdRS;
    @Mock private DebtServiceClient debtServiceClient;
    @Mock private NotificationServiceClient notificationServiceClient;

    @InjectMocks
    private AuthService authService;

    private static final String USER_ID     = "user123";
    private static final String EMAIL       = "test@example.com";
    private static final String PASSWORD    = "password123";
    private static final String HASH        = "$2a$hash";
    private static final String ACCESS_TOKEN   = "access.token";
    private static final String REFRESH_TOKEN  = "refresh.token";
    private static final String RESET_TOKEN    = "reset.token";

    private AuthDocument buildAuthDoc() {
        return AuthDocument.builder()
                .email(EMAIL)
                .password(HASH)
                .build();
    }

    private AuthDocument buildAuthDocWithId() {
        final AuthDocument doc = buildAuthDoc();
        // BaseDocument has no public setter for id in tests — use reflection helper
        org.springframework.test.util.ReflectionTestUtils.setField(doc, "id", USER_ID);
        return doc;
    }

    // ─── register ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("new email → saves user, fires event, returns tokens")
        void newEmail_returnsTokenResponse() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                when(authRS.existsByEmail(EMAIL)).thenReturn(false);
                when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);

                final AuthDocument saved = buildAuthDocWithId();
                when(authRS.save(any(AuthDocument.class))).thenReturn(saved);
                when(jwtService.generateToken(USER_ID, false, false)).thenReturn(ACCESS_TOKEN);
                when(jwtService.generateToken(USER_ID, true, false)).thenReturn(REFRESH_TOKEN);

                final RegisterRequest request = RegisterRequest.builder()
                        .email(EMAIL).password(PASSWORD).build();

                final TokenResponse response = authService.register(request);

                assertThat(response.getToken()).isEqualTo(ACCESS_TOKEN);
                assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
                verify(authRS).save(any(AuthDocument.class));
                verify(authEventProducer).produce(any(), any(), any());
            }
        }

        @Test
        @DisplayName("email already in use → throws CONFLICT")
        void emailInUse_throwsConflict() {
            when(authRS.existsByEmail(EMAIL)).thenReturn(true);

            final RegisterRequest request = RegisterRequest.builder()
                    .email(EMAIL).password(PASSWORD).build();

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> authService.register(request))
                    .matches(e -> e.getStatus() == HttpStatus.CONFLICT);

            verify(authRS, never()).save(any());
            verify(authEventProducer, never()).produce(any(), any(), any());
        }
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("valid credentials → returns tokens")
        void validCredentials_returnsTokenResponse() {
            final AuthDocument doc = buildAuthDocWithId();
            when(authRS.findByEMail(EMAIL)).thenReturn(doc);
            when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
            when(jwtService.generateToken(USER_ID, false, false)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateToken(USER_ID, true, false)).thenReturn(REFRESH_TOKEN);

            final LoginRequest request = LoginRequest.builder()
                    .email(EMAIL).password(PASSWORD).build();

            final TokenResponse response = authService.login(request);

            assertThat(response.getToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("wrong password → throws UNAUTHORIZED and increments rate limit counter")
        void wrongPassword_throwsUnauthorized() {
            final AuthDocument doc = buildAuthDoc();
            when(authRS.findByEMail(EMAIL)).thenReturn(doc);
            when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(false);

            final LoginRequest request = LoginRequest.builder()
                    .email(EMAIL).password(PASSWORD).build();

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> authService.login(request))
                    .matches(e -> e.getStatus() == HttpStatus.UNAUTHORIZED);

            verify(rateLimiterService).check(eq("login:" + EMAIL), eq(5), eq(900L), any());
        }

        @Test
        @DisplayName("rate limit exceeded → throws TOO_MANY_REQUESTS")
        void rateLimitExceeded_throwsTooManyRequests() {
            final AuthDocument doc = buildAuthDoc();
            when(authRS.findByEMail(EMAIL)).thenReturn(doc);
            when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(false);
            doThrow(BaseException.tooManyRequests("error.auth.too_many_login_attempts"))
                    .when(rateLimiterService).check(eq("login:" + EMAIL), eq(5), eq(900L), any());

            final LoginRequest request = LoginRequest.builder()
                    .email(EMAIL).password(PASSWORD).build();

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> authService.login(request))
                    .matches(e -> e.getStatus() == HttpStatus.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("successful login → resets rate limit counter")
        void successfulLogin_resetsRateLimit() {
            final AuthDocument doc = buildAuthDocWithId();
            when(authRS.findByEMail(EMAIL)).thenReturn(doc);
            when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
            when(jwtService.generateToken(any(), anyBoolean(), anyBoolean())).thenReturn(ACCESS_TOKEN);

            authService.login(LoginRequest.builder().email(EMAIL).password(PASSWORD).build());

            verify(rateLimiterService).reset("login:" + EMAIL);
        }
    }

    // ─── refreshToken ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refreshToken")
    class RefreshToken {

        @Test
        @DisplayName("valid Bearer header → returns new token pair")
        void validHeader_returnsTokenResponse() {
            when(tokenBlacklistService.isBlacklisted(REFRESH_TOKEN)).thenReturn(false);
            when(jwtService.validateRefreshToken(REFRESH_TOKEN)).thenReturn(USER_ID);
            when(authRS.findById(USER_ID)).thenReturn(buildAuthDocWithId());
            when(jwtService.generateToken(USER_ID, false, false)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateToken(USER_ID, true, false)).thenReturn(REFRESH_TOKEN);

            final TokenResponse response = authService.refreshToken("Bearer " + REFRESH_TOKEN);

            assertThat(response.getToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("null header → throws BAD_REQUEST")
        void nullHeader_throwsBadRequest() {
            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> authService.refreshToken(null))
                    .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("blank header → throws BAD_REQUEST")
        void blankHeader_throwsBadRequest() {
            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> authService.refreshToken("   "))
                    .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("missing Bearer prefix → throws BAD_REQUEST")
        void missingBearerPrefix_throwsBadRequest() {
            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                    .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Bearer with blank token → throws BAD_REQUEST")
        void bearerWithBlankToken_throwsBadRequest() {
            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> authService.refreshToken("Bearer   "))
                    .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("blacklisted token → throws UNAUTHORIZED")
        void blacklistedToken_throwsUnauthorized() {
            when(tokenBlacklistService.isBlacklisted(REFRESH_TOKEN)).thenReturn(true);

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> authService.refreshToken("Bearer " + REFRESH_TOKEN))
                    .matches(e -> e.getStatus() == HttpStatus.UNAUTHORIZED);
        }
    }

    // ─── forgetPassword ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("forgetPassword")
    class ForgetPassword {

        @Test
        @DisplayName("existing user → produces password reset event")
        void existingUser_producesEvent() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final AuthDocument doc = buildAuthDocWithId();
                when(authRS.findByEmailOptional(EMAIL)).thenReturn(Optional.of(doc));
                when(jwtService.generatePasswordResetToken(USER_ID)).thenReturn(RESET_TOKEN);

                final ForgetPasswordRequest request = new ForgetPasswordRequest();
                request.setEmail(EMAIL);

                authService.forgetPassword(request);

                verify(authEventProducer).produce(any(), any(), any());
            }
        }

        @Test
        @DisplayName("unknown email → silent no-op, no event")
        void unknownEmail_noOp() {
            when(authRS.findByEmailOptional(EMAIL)).thenReturn(Optional.empty());

            final ForgetPasswordRequest request = new ForgetPasswordRequest();
            request.setEmail(EMAIL);

            assertThatNoException().isThrownBy(() -> authService.forgetPassword(request));
            verify(authEventProducer, never()).produce(any(), any(), any());
        }

        @Test
        @DisplayName("rate limit exceeded → throws TOO_MANY_REQUESTS")
        void rateLimitExceeded_throwsTooManyRequests() {
            doThrow(BaseException.tooManyRequests("error.auth.too_many_password_reset_attempts"))
                    .when(rateLimiterService).check(eq("forget_pwd:" + EMAIL), eq(3), eq(3600L), any());

            final ForgetPasswordRequest request = new ForgetPasswordRequest();
            request.setEmail(EMAIL);

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> authService.forgetPassword(request))
                    .matches(e -> e.getStatus() == HttpStatus.TOO_MANY_REQUESTS);

            verify(authEventProducer, never()).produce(any(), any(), any());
        }
    }

    // ─── changePassword ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("correct current password → updates and saves")
        void correctCurrentPassword_updatesPassword() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);

                final AuthDocument doc = buildAuthDoc();
                when(authRS.findById(USER_ID)).thenReturn(doc);
                when(passwordEncoder.matches("oldPass", HASH)).thenReturn(true);
                when(passwordEncoder.encode("newPass")).thenReturn("$2a$newHash");
                when(authRS.save(doc)).thenReturn(doc);

                final ChangePasswordRequest request = new ChangePasswordRequest();
                request.setCurrentPassword("oldPass");
                request.setNewPassword("newPass");

                assertThatNoException().isThrownBy(() -> authService.changePassword(request));
                verify(authRS).save(doc);
                assertThat(doc.getPassword()).isEqualTo("$2a$newHash");
            }
        }

        @Test
        @DisplayName("wrong current password → throws UNAUTHORIZED")
        void wrongCurrentPassword_throwsUnauthorized() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);

                final AuthDocument doc = buildAuthDoc();
                when(authRS.findById(USER_ID)).thenReturn(doc);
                when(passwordEncoder.matches("wrongPass", HASH)).thenReturn(false);

                final ChangePasswordRequest request = new ChangePasswordRequest();
                request.setCurrentPassword("wrongPass");
                request.setNewPassword("newPass");

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> authService.changePassword(request))
                        .matches(e -> e.getStatus() == HttpStatus.UNAUTHORIZED);

                verify(authRS, never()).save(any());
            }
        }
    }

    // ─── resetPassword ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("valid reset token → updates and saves password")
        void validResetToken_updatesPassword() {
            when(tokenBlacklistService.isBlacklisted(RESET_TOKEN)).thenReturn(false);
            when(jwtService.validatePasswordResetToken(RESET_TOKEN)).thenReturn(USER_ID);

            final AuthDocument doc = buildAuthDoc();
            when(authRS.findById(USER_ID)).thenReturn(doc);
            when(passwordEncoder.encode("newPass")).thenReturn("$2a$newHash");
            when(authRS.save(doc)).thenReturn(doc);
            when(jwtService.getRemainingTtlSeconds(RESET_TOKEN)).thenReturn(300L);

            final ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken(RESET_TOKEN);
            request.setNewPassword("newPass");

            assertThatNoException().isThrownBy(() -> authService.resetPassword(request));
            verify(authRS).save(doc);
            verify(tokenBlacklistService).blacklist(RESET_TOKEN, 300L);
            assertThat(doc.getPassword()).isEqualTo("$2a$newHash");
        }

        @Test
        @DisplayName("already used reset token → throws UNAUTHORIZED")
        void alreadyUsedToken_throwsUnauthorized() {
            when(tokenBlacklistService.isBlacklisted(RESET_TOKEN)).thenReturn(true);

            final ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken(RESET_TOKEN);
            request.setNewPassword("newPass");

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> authService.resetPassword(request))
                    .matches(e -> e.getStatus() == HttpStatus.UNAUTHORIZED);

            verify(authRS, never()).save(any());
        }
    }

    // ─── logout ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("valid Bearer refresh token → blacklists token")
        void validRefreshToken_blacklistsToken() {
            when(jwtService.getRemainingTtlSeconds(REFRESH_TOKEN)).thenReturn(3600L);

            assertThatNoException().isThrownBy(() -> authService.logout("Bearer " + REFRESH_TOKEN));

            verify(tokenBlacklistService).blacklist(REFRESH_TOKEN, 3600L);
        }

        @Test
        @DisplayName("null header → throws BAD_REQUEST")
        void nullHeader_throwsBadRequest() {
            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> authService.logout(null))
                    .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("missing Bearer prefix → throws BAD_REQUEST")
        void missingBearerPrefix_throwsBadRequest() {
            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> authService.logout(REFRESH_TOKEN))
                    .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Bearer with blank token → throws BAD_REQUEST")
        void bearerWithBlankToken_throwsBadRequest() {
            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> authService.logout("Bearer   "))
                    .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);
        }
    }

    // ─── deleteAccount ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccount {

        @Test
        @DisplayName("authenticated user → cascades to debt/notification services, deletes household link and account")
        void authenticatedUser_cascadesAndDeletesAccount() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);

                final HouseholdDocument household = HouseholdDocument.builder()
                        .ownerId(USER_ID)
                        .partnerId("partner1")
                        .build();
                when(householdRS.findAllForUser(USER_ID)).thenReturn(java.util.List.of(household));

                authService.deleteAccount();

                verify(debtServiceClient).deleteAccountData(USER_ID);
                verify(notificationServiceClient).deleteAccountData(USER_ID);
                verify(householdRS).delete(household);
                verify(authRS).delete(USER_ID);
            }
        }

        @Test
        @DisplayName("no household link → skips household deletion but still deletes account")
        void noHousehold_skipsHouseholdDeletion() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);
                when(householdRS.findAllForUser(USER_ID)).thenReturn(java.util.List.of());

                authService.deleteAccount();

                verify(householdRS, never()).delete(any());
                verify(authRS).delete(USER_ID);
            }
        }

        @Test
        @DisplayName("debt-service unavailable → propagates exception, account not deleted")
        void debtServiceUnavailable_doesNotDeleteAccount() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);
                doThrow(BaseException.serviceUnavailable("error.debt_service.unavailable"))
                        .when(debtServiceClient).deleteAccountData(USER_ID);

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> authService.deleteAccount());

                verify(authRS, never()).delete(any());
            }
        }
    }

    // ─── userDetail ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("userDetail")
    class UserDetail {

        @Test
        @DisplayName("authenticated user → returns UserDetailResponse with email, premium, createdAt")
        void authenticatedUser_returnsUserDetail() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);

                final AuthDocument doc = buildAuthDocWithId();
                org.springframework.test.util.ReflectionTestUtils.setField(doc, "createdDate",
                        java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
                when(authRS.findById(USER_ID)).thenReturn(doc);

                final UserDetailResponse response = authService.userDetail();

                assertThat(response.getEmail()).isEqualTo(EMAIL);
                assertThat(response.isPremium()).isFalse();
                assertThat(response.getCreatedAt()).isEqualTo(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
            }
        }
    }
}
