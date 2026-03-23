package dev.batuhanyetgin.authservice.controller;

import dev.batuhanyetgin.authservice.dto.request.ChangePasswordRequest;
import dev.batuhanyetgin.authservice.dto.request.ForgetPasswordRequest;
import dev.batuhanyetgin.authservice.dto.request.LoginRequest;
import dev.batuhanyetgin.authservice.dto.request.RegisterRequest;
import dev.batuhanyetgin.authservice.dto.request.ResetPasswordRequest;
import dev.batuhanyetgin.authservice.dto.response.UserDetailResponse;
import dev.batuhanyetgin.authservice.dto.response.TokenResponse;
import dev.batuhanyetgin.authservice.repositoryservice.AuthRepositoryService;
import dev.batuhanyetgin.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth/")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login and password management")
public class AuthController {

    private final AuthService authService;
    private final AuthRepositoryService authRS;

    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered, tokens returned"),
            @ApiResponse(responseCode = "409", description = "Email already in use"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PostMapping("register")
    ResponseEntity<TokenResponse> register(@RequestBody @Valid final RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(summary = "Login with email and password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, tokens returned"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PostMapping("login")
    ResponseEntity<TokenResponse> login(@RequestBody @Valid final LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Refresh access token using a refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New token pair returned"),
            @ApiResponse(responseCode = "400", description = "Missing or malformed Authorization header"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("refresh-token")
    ResponseEntity<TokenResponse> refreshToken(@RequestHeader("Authorization") final String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    @Operation(summary = "Request a password reset email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset email sent if account exists"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PostMapping("forget-password")
    ResponseEntity<Void> forgetPassword(@RequestBody @Valid final ForgetPasswordRequest request) {
        authService.forgetPassword(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Change password for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "401", description = "Current password is incorrect"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PostMapping("change-password")
    ResponseEntity<Void> changePassword(@RequestBody @Valid final ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reset password using a token from the reset email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired reset token"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PostMapping("reset-password")
    ResponseEntity<Void> resetPassword(@RequestBody @Valid final ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get the authenticated user's profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("user-detail")
    ResponseEntity<UserDetailResponse> userDetail() {
        return ResponseEntity.ok(authService.userDetail());
    }

    @Operation(summary = "Check if a user exists by ID (internal inter-service use)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Returns true if user exists, false otherwise")
    })
    @GetMapping("user/exists/{userId}")
    ResponseEntity<Boolean> userExists(@PathVariable final String userId) {
        return ResponseEntity.ok(authRS.existsById(userId));
    }
}