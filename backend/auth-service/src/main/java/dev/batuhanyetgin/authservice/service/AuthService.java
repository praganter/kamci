package dev.batuhanyetgin.authservice.service;

import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.dto.request.ForgetPasswordRequest;
import dev.batuhanyetgin.authservice.dto.request.LoginRequest;
import dev.batuhanyetgin.authservice.dto.request.RegisterRequest;
import dev.batuhanyetgin.authservice.dto.request.ResetPasswordRequest;
import dev.batuhanyetgin.authservice.dto.response.LoginResponse;
import dev.batuhanyetgin.authservice.dto.response.RefreshTokenResponse;
import dev.batuhanyetgin.authservice.dto.response.RegisterResponse;
import dev.batuhanyetgin.authservice.producer.AuthEventProducer;
import dev.batuhanyetgin.authservice.repositoryservice.AuthRepositoryService;
import dev.batuhanyetgin.commonmodule.event.PasswordResetEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.commonmodule.event.UserRegisteredEvent;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthRepositoryService authRS;
    private final AuthEventProducer authEventProducer;

    public RegisterResponse register(final RegisterRequest request) {

        if (authRS.existsByEmail(request.getEmail())) {
            throw new BaseException("auth.email.in.use", HttpStatus.CONFLICT);
        }

        AuthDocument authDocument = AuthDocument.builder()
                .email(request.getEmail())
                .password(encodePassword(request.getPassword()))
                .build();

        authDocument = authRS.save(authDocument);

        authEventProducer.produce(
                RabbitMQConstants.NOTIFICATION_EXCHANGE,
                RabbitMQConstants.USER_REGISTERED_ROUTING_KEY,
                UserRegisteredEvent.builder()
                        .userId(authDocument.getId())
                        .email(authDocument.getEmail())
                        .build()
        );

        return RegisterResponse.builder()
                .refreshToken(jwtService.generateToken(authDocument.getId(), true))
                .token(jwtService.generateToken(authDocument.getId(), false))
                .build();
    }

    public void forgetPassword(final ForgetPasswordRequest request) {
        final AuthDocument authDocument = authRS.findByEMail(request.getEmail());

        final String resetToken = jwtService.generatePasswordResetToken(authDocument.getId());

        authEventProducer.produce(
                RabbitMQConstants.NOTIFICATION_EXCHANGE,
                RabbitMQConstants.PASSWORD_RESET_ROUTING_KEY,
                PasswordResetEvent.builder()
                        .userId(authDocument.getId())
                        .email(authDocument.getEmail())
                        .resetToken(resetToken)
                        .build()
        );

        log.info("Password reset event produced for userId: {}", authDocument.getId());
    }

    public void resetPassword(final ResetPasswordRequest request) {
        final String userId = jwtService.validatePasswordResetToken(request.getToken());
        final AuthDocument authDocument = authRS.findById(userId);

        authDocument.setPassword(encodePassword(request.getNewPassword()));
        authRS.save(authDocument);

        log.info("Password reset successfully for userId: {}", userId);
    }

    public LoginResponse login(final LoginRequest request) throws BaseException {
        final AuthDocument userDocument = authRS.findByEMail(request.getEmail());

        if (userDocument == null) {
            throw new BaseException("error.auth.user_not_found", HttpStatus.NOT_FOUND);
        }

        if (isPasswordValid(request.getPassword(), userDocument.getPassword())) {
            return LoginResponse.builder()
                    .refreshToken(jwtService.generateToken(userDocument.getId(), true))
                    .token(jwtService.generateToken(userDocument.getId(), false))
                    .build();
        } else {
            throw new BaseException("error.auth.not_valid", HttpStatus.UNAUTHORIZED);
        }
    }

    public RefreshTokenResponse refreshToken(final String authorizationHeader) {

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new BaseException("error.request.invalid_header", HttpStatus.BAD_REQUEST);
        }

        final String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            throw new BaseException("error.request.invalid_header", HttpStatus.BAD_REQUEST);
        }

        final String token = authorizationHeader.substring(prefix.length()).trim();
        if (token.isBlank()) {
            throw new BaseException("error.request.invalid_header", HttpStatus.BAD_REQUEST);
        }

        final String userId = jwtService.getUserIdFromToken(token);

        return RefreshTokenResponse.builder()
                .refreshToken(jwtService.generateToken(userId, true))
                .token(userId != null ? jwtService.generateToken(userId, false) : null)
                .build();
    }

    private String encodePassword(final String password) {
        return passwordEncoder.encode(password);
    }

    private boolean isPasswordValid(final String password, final String hashPassword) {
        return passwordEncoder.matches(password, hashPassword);
    }
}