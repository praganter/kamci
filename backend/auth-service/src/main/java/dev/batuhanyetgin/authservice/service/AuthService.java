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
import dev.batuhanyetgin.commonmodule.event.PasswordResetEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.commonmodule.event.UserRegisteredEvent;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public TokenResponse register(final RegisterRequest request) {

        if (authRS.existsByEmail(request.getEmail())) {
            throw BaseException.conflict("error.auth.email_in_use");
        }

        AuthDocument authDocument = AuthDocument.builder()
                .email(request.getEmail())
                .password(encodePassword(request.getPassword()))
                .build();

        authDocument = authRS.save(authDocument);

        log.info("User registered successfully with id: {}", authDocument.getId());

        authEventProducer.produce(
                RabbitMQConstants.NOTIFICATION_EXCHANGE,
                RabbitMQConstants.USER_REGISTERED_ROUTING_KEY,
                UserRegisteredEvent.builder()
                        .userId(authDocument.getId())
                        .email(authDocument.getEmail())
                        .locale(RequestContextUtil.getLocale())
                        .build()
        );

        return TokenResponse.builder()
                .refreshToken(jwtService.generateToken(authDocument.getId(), true))
                .token(jwtService.generateToken(authDocument.getId(), false))
                .build();
    }

    public void forgetPassword(final ForgetPasswordRequest request) {
        authRS.findByEmailOptional(request.getEmail()).ifPresent(authDocument -> {
            final String resetToken = jwtService.generatePasswordResetToken(authDocument.getId());

            authEventProducer.produce(
                    RabbitMQConstants.NOTIFICATION_EXCHANGE,
                    RabbitMQConstants.PASSWORD_RESET_ROUTING_KEY,
                    PasswordResetEvent.builder()
                            .userId(authDocument.getId())
                            .email(authDocument.getEmail())
                            .resetToken(resetToken)
                            .locale(RequestContextUtil.getLocale())
                            .build()
            );

            log.info("Password reset event produced for userId: {}", authDocument.getId());
        });
    }

    public void changePassword(final ChangePasswordRequest request) {
        final String userId = RequestContextUtil.getUserId();
        final AuthDocument authDocument = authRS.findById(userId);

        if (!isPasswordValid(request.getCurrentPassword(), authDocument.getPassword())) {
            throw BaseException.unauthorized("error.auth.current_password_invalid");
        }

        authDocument.setPassword(encodePassword(request.getNewPassword()));
        authRS.save(authDocument);

        log.info("Password changed successfully for userId: {}", userId);
    }

    public void resetPassword(final ResetPasswordRequest request) {
        final String userId = jwtService.validatePasswordResetToken(request.getToken());
        final AuthDocument authDocument = authRS.findById(userId);

        authDocument.setPassword(encodePassword(request.getNewPassword()));
        authRS.save(authDocument);

        log.info("Password reset successfully for userId: {}", userId);
    }

    public TokenResponse login(final LoginRequest request) {
        final AuthDocument userDocument = authRS.findByEMail(request.getEmail());

        if (!isPasswordValid(request.getPassword(), userDocument.getPassword())) {
            throw BaseException.unauthorized("error.auth.not_valid");
        }

        log.info("User logged in successfully with id: {}", userDocument.getId());

        return TokenResponse.builder()
                .refreshToken(jwtService.generateToken(userDocument.getId(), true))
                .token(jwtService.generateToken(userDocument.getId(), false))
                .build();
    }

    public TokenResponse refreshToken(final String authorizationHeader) {

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw BaseException.badRequest("error.request.invalid_header");
        }

        final String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            throw BaseException.badRequest("error.request.invalid_header");
        }

        final String token = authorizationHeader.substring(prefix.length()).trim();
        if (token.isBlank()) {
            throw BaseException.badRequest("error.request.invalid_header");
        }

        final String userId = jwtService.validateRefreshToken(token);

        log.info("Token refreshed for userId: {}", userId);

        return TokenResponse.builder()
                .refreshToken(jwtService.generateToken(userId, true))
                .token(jwtService.generateToken(userId, false))
                .build();
    }

    private String encodePassword(final String password) {
        return passwordEncoder.encode(password);
    }

    private boolean isPasswordValid(final String password, final String hashPassword) {
        return passwordEncoder.matches(password, hashPassword);
    }
}