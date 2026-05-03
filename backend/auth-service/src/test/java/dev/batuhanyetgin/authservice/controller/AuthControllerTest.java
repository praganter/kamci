package dev.batuhanyetgin.authservice.controller;

import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.dto.request.*;
import dev.batuhanyetgin.authservice.dto.response.TokenResponse;
import dev.batuhanyetgin.authservice.dto.response.UserDetailResponse;
import dev.batuhanyetgin.authservice.repositoryservice.AuthRepositoryService;
import dev.batuhanyetgin.authservice.service.AuthService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private AuthRepositoryService authRS;

    @InjectMocks
    private AuthController controller;

    private static final String USER_ID = "user123";
    private static final String EMAIL = "test@example.com";
    private static final TokenResponse TOKEN = TokenResponse.builder()
            .token("access").refreshToken("refresh").build();

    @Nested
    class Register {

        @Test
        void delegatesToServiceAndReturns200() {
            final RegisterRequest req = RegisterRequest.builder()
                    .email(EMAIL).password("password1").build();
            when(authService.register(req)).thenReturn(TOKEN);

            final ResponseEntity<TokenResponse> response = controller.register(req);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(TOKEN);
            verify(authService).register(req);
        }
    }

    @Nested
    class Login {

        @Test
        void delegatesToServiceAndReturns200() {
            final LoginRequest req = LoginRequest.builder()
                    .email(EMAIL).password("password1").build();
            when(authService.login(req)).thenReturn(TOKEN);

            final ResponseEntity<TokenResponse> response = controller.login(req);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(TOKEN);
            verify(authService).login(req);
        }
    }

    @Nested
    class RefreshToken {

        @Test
        void delegatesToServiceAndReturns200() {
            final String header = "Bearer refresh-token";
            when(authService.refreshToken(header)).thenReturn(TOKEN);

            final ResponseEntity<TokenResponse> response = controller.refreshToken(header);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(TOKEN);
        }
    }

    @Nested
    class ForgetPassword {

        @Test
        void delegatesToServiceAndReturns200() {
            final ForgetPasswordRequest req = new ForgetPasswordRequest();

            final ResponseEntity<Void> response = controller.forgetPassword(req);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(authService).forgetPassword(req);
        }
    }

    @Nested
    class ChangePassword {

        @Test
        void delegatesToServiceAndReturns204() {
            final ChangePasswordRequest req = new ChangePasswordRequest();

            final ResponseEntity<Void> response = controller.changePassword(req);

            assertThat(response.getStatusCode().value()).isEqualTo(204);
            verify(authService).changePassword(req);
        }
    }

    @Nested
    class ResetPassword {

        @Test
        void delegatesToServiceAndReturns200() {
            final ResetPasswordRequest req = new ResetPasswordRequest();

            final ResponseEntity<Void> response = controller.resetPassword(req);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(authService).resetPassword(req);
        }
    }

    @Nested
    class UserDetail {

        @Test
        void delegatesToServiceAndReturns200() {
            final UserDetailResponse detail = UserDetailResponse.builder().email(EMAIL).build();
            when(authService.userDetail()).thenReturn(detail);

            final ResponseEntity<UserDetailResponse> response = controller.userDetail();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getEmail()).isEqualTo(EMAIL);
        }
    }

    @Nested
    class UserExists {

        @Test
        void existingUser_returnsTrue() {
            when(authRS.existsById(USER_ID)).thenReturn(true);

            final ResponseEntity<Boolean> response = controller.userExists(USER_ID);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isTrue();
        }

        @Test
        void missingUser_returnsFalse() {
            when(authRS.existsById(USER_ID)).thenReturn(false);

            final ResponseEntity<Boolean> response = controller.userExists(USER_ID);

            assertThat(response.getBody()).isFalse();
        }
    }

    @Nested
    class GetUserIdByEmail {

        @Test
        void returnsUserIdFromDocument() {
            final AuthDocument doc = AuthDocument.builder().email(EMAIL).build();
            doc.setId(USER_ID);
            when(authRS.findByEMail(EMAIL)).thenReturn(doc);

            final ResponseEntity<String> response = controller.getUserIdByEmail(EMAIL);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(USER_ID);
        }
    }
}
