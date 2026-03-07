package dev.batuhanyetgin.authservice.service;

import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.dto.request.LoginRequest;
import dev.batuhanyetgin.authservice.dto.request.RegisterRequest;
import dev.batuhanyetgin.authservice.dto.response.LoginResponse;
import dev.batuhanyetgin.authservice.dto.response.RefreshTokenResponse;
import dev.batuhanyetgin.authservice.dto.response.RegisterResponse;
import dev.batuhanyetgin.authservice.repositoryservice.AuthRepositoryService;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD = "password123";
    private static final String HASHED_PASSWORD = "hashed-password";
    private static final String USER_ID = "user-123";
    private static final String TOKEN = "test-token";
    private static final String REFRESH_TOKEN = "test-refresh-token";
    @InjectMocks
    private AuthService authService;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthRepositoryService authRS;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Test
    void register_ShouldReturnRegisterResponse_WhenRequestIsValid() {
        final RegisterRequest request = RegisterRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build();

        final AuthDocument savedDocument = AuthDocument.builder()
                .id(USER_ID)
                .email(EMAIL)
                .password(HASHED_PASSWORD)
                .build();

        when(authRS.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(authRS.save(any(AuthDocument.class))).thenReturn(savedDocument);
        when(jwtService.generateToken(USER_ID, false)).thenReturn(TOKEN);
        when(jwtService.generateToken(USER_ID, true)).thenReturn(REFRESH_TOKEN);

        final RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(TOKEN, response.getToken());
        assertEquals(REFRESH_TOKEN, response.getRefreshToken());
        verify(authRS).existsByEmail(EMAIL);
        verify(passwordEncoder).encode(PASSWORD);
        verify(authRS).save(any(AuthDocument.class));
        verify(jwtService, times(2)).generateToken(anyString(), anyBoolean());
    }

    @Test
    void login_ShouldReturnLoginResponse_WhenCredentialsAreValid() {
        final LoginRequest request = LoginRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build();

        final AuthDocument userDocument = AuthDocument.builder()
                .id(USER_ID)
                .email(EMAIL)
                .password(HASHED_PASSWORD)
                .build();

        when(authRS.findByEMail(EMAIL)).thenReturn(userDocument);
        when(passwordEncoder.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(jwtService.generateToken(USER_ID, false)).thenReturn(TOKEN);
        when(jwtService.generateToken(USER_ID, true)).thenReturn(REFRESH_TOKEN);

        final LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(TOKEN, response.getToken());
        assertEquals(REFRESH_TOKEN, response.getRefreshToken());
        verify(authRS).findByEMail(EMAIL);
        verify(passwordEncoder).matches(PASSWORD, HASHED_PASSWORD);
        verify(jwtService, times(2)).generateToken(anyString(), anyBoolean());
    }

    @Test
    void login_ShouldThrowBaseException_WhenPasswordIsInvalid() {
        final LoginRequest request = LoginRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build();

        final AuthDocument userDocument = AuthDocument.builder()
                .id(USER_ID)
                .email(EMAIL)
                .password(HASHED_PASSWORD)
                .build();

        when(authRS.findByEMail(EMAIL)).thenReturn(userDocument);
        when(passwordEncoder.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(false);

        final BaseException exception = assertThrows(BaseException.class, () -> authService.login(request));

        assertEquals("error.auth.not_valid", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(authRS).findByEMail(EMAIL);
        verify(passwordEncoder).matches(PASSWORD, HASHED_PASSWORD);
        verifyNoInteractions(jwtService);
    }

    @Test
    void refreshToken_ShouldReturnRefreshTokenResponse_WhenHeaderIsValid() {
        final String authHeader = "Bearer " + TOKEN;
        when(jwtService.getUserIdFromToken(TOKEN)).thenReturn(USER_ID);
        when(jwtService.generateToken(USER_ID, false)).thenReturn("new-token");
        when(jwtService.generateToken(USER_ID, true)).thenReturn("new-refresh-token");

        final RefreshTokenResponse response = authService.refreshToken(authHeader);

        assertNotNull(response);
        assertEquals("new-token", response.getToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        verify(jwtService).getUserIdFromToken(TOKEN);
        verify(jwtService, times(2)).generateToken(anyString(), anyBoolean());
    }
}