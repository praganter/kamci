package dev.batuhanyetgin.authservice.service;

import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.dto.request.ChangePasswordRequest;
import dev.batuhanyetgin.authservice.dto.request.ForgetPasswordRequest;
import dev.batuhanyetgin.authservice.dto.request.LoginRequest;
import dev.batuhanyetgin.authservice.dto.request.RegisterRequest;
import dev.batuhanyetgin.authservice.dto.request.ResetPasswordRequest;
import dev.batuhanyetgin.authservice.dto.response.TokenResponse;
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
                when(jwtService.generateToken(USER_ID, false)).thenReturn(ACCESS_TOKEN);
                when(jwtService.generateToken(USER_ID, true)).thenReturn(REFRESH_TOKEN);

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
            when(jwtService.generateToken(USER_ID, false)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateToken(USER_ID, true)).thenReturn(REFRESH_TOKEN);

            final LoginRequest request = LoginRequest.builder()
                    .email(EMAIL).password(PASSWORD).build();

            final TokenResponse response = authService.login(request);

            assertThat(response.getToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("wrong password → throws UNAUTHORIZED")
        void wrongPassword_throwsUnauthorized() {
            final AuthDocument doc = buildAuthDoc();
            when(authRS.findByEMail(EMAIL)).thenReturn(doc);
            when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(false);

            final LoginRequest request = LoginRequest.builder()
                    .email(EMAIL).password(PASSWORD).build();

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> authService.login(request))
                    .matches(e -> e.getStatus() == HttpStatus.UNAUTHORIZED);
        }
    }

    // ─── refreshToken ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refreshToken")
    class RefreshToken {

        @Test
        @DisplayName("valid Bearer header → returns new token pair")
        void validHeader_returnsTokenResponse() {
            when(jwtService.validateRefreshToken(REFRESH_TOKEN)).thenReturn(USER_ID);
            when(jwtService.generateToken(USER_ID, false)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateToken(USER_ID, true)).thenReturn(REFRESH_TOKEN);

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
            when(jwtService.validatePasswordResetToken(RESET_TOKEN)).thenReturn(USER_ID);

            final AuthDocument doc = buildAuthDoc();
            when(authRS.findById(USER_ID)).thenReturn(doc);
            when(passwordEncoder.encode("newPass")).thenReturn("$2a$newHash");
            when(authRS.save(doc)).thenReturn(doc);

            final ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken(RESET_TOKEN);
            request.setNewPassword("newPass");

            assertThatNoException().isThrownBy(() -> authService.resetPassword(request));
            verify(authRS).save(doc);
            assertThat(doc.getPassword()).isEqualTo("$2a$newHash");
        }
    }
}
