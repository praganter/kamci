package dev.batuhanyetgin.authservice.exception;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthExceptionHandlerTest {

    @Mock
    private MessageSource messageSource;

    private AuthExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new AuthExceptionHandler(messageSource);
        request = new MockHttpServletRequest();
        request.setRequestURI("/auth/login");
    }

    @Test
    @DisplayName("JwtException → 401 UNAUTHORIZED with localized message")
    void jwtException_returnsUnauthorized() {
        when(messageSource.getMessage(any(), any(), any(), any(Locale.class))).thenReturn("Token is invalid");

        final var response = handler.handleJwtException(
                new ExpiredJwtException(null, null, "expired"), Locale.ENGLISH, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getCode()).isEqualTo("error.auth.token_invalid");
        assertThat(response.getBody().getMessage()).isEqualTo("Token is invalid");
        assertThat(response.getBody().getPath()).isEqualTo("/auth/login");
    }
}
