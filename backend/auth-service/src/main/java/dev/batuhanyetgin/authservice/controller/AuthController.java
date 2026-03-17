package dev.batuhanyetgin.authservice.controller;

import dev.batuhanyetgin.authservice.dto.request.ForgetPasswordRequest;
import dev.batuhanyetgin.authservice.dto.request.LoginRequest;
import dev.batuhanyetgin.authservice.dto.request.RegisterRequest;
import dev.batuhanyetgin.authservice.dto.request.ResetPasswordRequest;
import dev.batuhanyetgin.authservice.dto.response.LoginResponse;
import dev.batuhanyetgin.authservice.dto.response.RefreshTokenResponse;
import dev.batuhanyetgin.authservice.dto.response.RegisterResponse;
import dev.batuhanyetgin.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth/")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("register")
    ResponseEntity<RegisterResponse> save(@RequestBody @Valid final RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("login")
    ResponseEntity<LoginResponse> save(@RequestBody @Valid final LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("refresh-token")
    ResponseEntity<RefreshTokenResponse> refreshToken(@RequestHeader("Authorization") final String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    @PostMapping("forget-password")
    ResponseEntity<Void> forgetPassword(@RequestBody @Valid final ForgetPasswordRequest request) {
        authService.forgetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("reset-password")
    ResponseEntity<Void> resetPassword(@RequestBody @Valid final ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}