package com.swiftroute.controller;

import com.swiftroute.common.ApiResponse;
import com.swiftroute.dto.request.LoginRequest;
import com.swiftroute.dto.request.RefreshTokenRequest;
import com.swiftroute.dto.response.AuthResponse;
import com.swiftroute.dto.response.UserResponse;
import com.swiftroute.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication & RBAC", description = "Endpoints for user login, JWT token issuance, refresh, and profile retrieval")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates credentials and returns JWT access and refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token", description = "Validates a refresh token and generates a new JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Current User Profile", description = "Returns the profile of the currently authenticated user",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UserResponse response = authService.getCurrentUser(authentication);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
