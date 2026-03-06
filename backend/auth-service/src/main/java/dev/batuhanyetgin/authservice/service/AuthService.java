package dev.batuhanyetgin.authservice.service;

import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.dto.request.LoginRequest;
import dev.batuhanyetgin.authservice.dto.request.RegisterRequest;
import dev.batuhanyetgin.authservice.dto.response.LoginResponse;
import dev.batuhanyetgin.authservice.dto.response.RefreshTokenResponse;
import dev.batuhanyetgin.authservice.dto.response.RegisterResponse;
import dev.batuhanyetgin.authservice.repositoryservice.AuthRepositoryService;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AuthRepositoryService authRS;

    public RegisterResponse register(final RegisterRequest request) {

        if (authRS.existsByEmail(request.getEmail())) {
            throw new BaseException("auth.email.in.use", HttpStatus.CONFLICT);
        }

        AuthDocument authDocument = AuthDocument.builder()
                .email(request.getEmail())
                .password(encodePassword(request.getPassword()))
                .build();

        authDocument = authRS.save(authDocument);

        return RegisterResponse.builder()
                .refreshToken(jwtService.generateToken(authDocument.getId(), true))
                .token(jwtService.generateToken(authDocument.getId(), false))
                .build();
    }

    public LoginResponse login(final LoginRequest request) throws BaseException {
        AuthDocument userDocument = authRS.findByEMail(request.getEmail());

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

        String token = authorizationHeader.substring(prefix.length()).trim();
        if (token.isBlank()) {
            throw new BaseException("error.request.invalid_header", HttpStatus.BAD_REQUEST);
        }

        String userId = jwtService.getUserIdFromToken(token);

        return RefreshTokenResponse.builder()
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