package com.legalmetrology.auth.controller;

import com.legalmetrology.auth.dto.AuthResponse;
import com.legalmetrology.auth.dto.ForgotPasswordRequest;
import com.legalmetrology.auth.dto.LoginRequest;
import com.legalmetrology.auth.dto.RefreshTokenRequest;
import com.legalmetrology.auth.dto.ResetPasswordRequest;
import com.legalmetrology.auth.dto.SignupRequest;
import com.legalmetrology.auth.dto.TokenRefreshResponse;
import com.legalmetrology.auth.dto.UserResponse;
import com.legalmetrology.auth.mapper.UserMapper;
import com.legalmetrology.auth.repository.UserRepository;
import com.legalmetrology.auth.service.AuthService;
import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.security.SecurityUtils;
import com.legalmetrology.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Signup, login, token refresh, logout, and password recovery")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Operation(summary = "Register a new inspector account",
            description = "Creates a new user with the default INSPECTOR role and immediately issues an access/refresh " +
                    "token pair, so the caller does not need a separate login call. Fails if the email or employee code " +
                    "is already registered. Authorization: none — this endpoint is public.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Account created; tokens issued",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Resource created successfully",
                              "data": {
                                "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.abc123",
                                "refreshToken": "8f14e45f-ceea-467e-9986-4b8c1a2f3c6e",
                                "tokenType": "Bearer",
                                "expiresInMs": 900000,
                                "user": {
                                  "id": "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                  "fullName": "Priya Sharma",
                                  "email": "priya.sharma@legalmetrology.gov.in",
                                  "employeeCode": "LM-2024-0142",
                                  "phone": "+91-9876543210",
                                  "preferredLocale": "en-IN",
                                  "active": true,
                                  "roles": ["INSPECTOR"],
                                  "createdAt": "2026-01-15T10:30:00Z"
                                }
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation failed — e.g. blank full name/email/password, malformed email, or password not meeting complexity rules"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "An account with this email or employee code already exists",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "message": "An account with this email already exists", "timestamp": "2026-01-15T10:30:00Z"}
                            """)))
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseUtil.created(authService.signup(request));
    }

    @Operation(summary = "Authenticate with email and password",
            description = "Verifies credentials and issues a fresh access/refresh token pair. " +
                    "Authorization: none — this endpoint is public.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Login successful",
                              "data": {
                                "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.abc123",
                                "refreshToken": "8f14e45f-ceea-467e-9986-4b8c1a2f3c6e",
                                "tokenType": "Bearer",
                                "expiresInMs": 900000,
                                "user": {
                                  "id": "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                  "fullName": "Priya Sharma",
                                  "email": "priya.sharma@legalmetrology.gov.in",
                                  "employeeCode": "LM-2024-0142",
                                  "phone": "+91-9876543210",
                                  "preferredLocale": "en-IN",
                                  "active": true,
                                  "roles": ["INSPECTOR"],
                                  "createdAt": "2026-01-15T10:30:00Z"
                                }
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation failed — email or password missing/malformed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Invalid email or password, or the account is disabled",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "message": "Invalid email or password", "timestamp": "2026-01-15T10:30:00Z"}
                            """)))
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseUtil.ok("Login successful", authService.login(request));
    }

    @Operation(summary = "Exchange a valid refresh token for a new access/refresh token pair",
            description = "Rotates the refresh token: the supplied token is revoked and a brand-new one is issued, so " +
                    "a leaked refresh token cannot be replayed after legitimate use. Authorization: none — this endpoint is public.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "New token pair issued",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": {
                                "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.def456",
                                "refreshToken": "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                "tokenType": "Bearer",
                                "expiresInMs": 900000
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation failed — refreshToken missing/blank"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "The refresh token is invalid, expired, or already revoked — the caller must log in again",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "message": "Refresh token has expired or been revoked. Please log in again.", "timestamp": "2026-01-15T10:30:00Z"}
                            """)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseUtil.ok(authService.refreshToken(request.refreshToken()));
    }

    @Operation(summary = "Revoke a refresh token, ending the session it belongs to",
            description = "Idempotent: if the token is already revoked or unknown, this still returns success. " +
                    "Authorization: none — this endpoint is public (it only requires knowledge of the refresh token itself).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": true, "message": "Logged out successfully", "timestamp": "2026-01-15T10:30:00Z"}
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation failed — refreshToken missing/blank")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseUtil.noContent("Logged out successfully");
    }

    @Operation(summary = "Request a password reset token for the given email",
            description = "Always returns the same success response whether or not the email is registered, so this " +
                    "endpoint cannot be used to enumerate accounts. The reset token itself is delivered out-of-band " +
                    "(logged only in non-production profiles — email delivery is not yet wired up). " +
                    "Authorization: none — this endpoint is public.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request accepted (does not confirm the email exists)",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": true, "message": "If an account exists for this email, a password reset link has been sent", "timestamp": "2026-01-15T10:30:00Z"}
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation failed — email missing or malformed")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseUtil.noContent("If an account exists for this email, a password reset link has been sent");
    }

    @Operation(summary = "Complete a password reset using the token issued via forgot-password",
            description = "Consumes the reset token (single use) and revokes all of the user's existing refresh tokens, " +
                    "forcing re-login on every other device. Authorization: none — this endpoint is public.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password has been reset successfully",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": true, "message": "Password has been reset successfully", "timestamp": "2026-01-15T10:30:00Z"}
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation failed (missing token/password), or the token is invalid, expired, or already used",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "message": "Invalid or expired password reset token", "timestamp": "2026-01-15T10:30:00Z"}
                            """)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseUtil.noContent("Password has been reset successfully");
    }

    @Operation(summary = "Get the profile of the currently authenticated user",
            description = "Resolves the user from the bearer token's subject claim. " +
                    "Authorization: any authenticated user (despite living under the public /api/v1/auth/** path prefix, " +
                    "this specific endpoint requires a valid access token — it fails with 401 if none is supplied).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current user's profile",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "message": "Request completed successfully",
                              "data": {
                                "id": "b3f1c2d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
                                "fullName": "Priya Sharma",
                                "email": "priya.sharma@legalmetrology.gov.in",
                                "employeeCode": "LM-2024-0142",
                                "phone": "+91-9876543210",
                                "preferredLocale": "en-IN",
                                "active": true,
                                "roles": ["INSPECTOR"],
                                "createdAt": "2026-01-15T10:30:00Z"
                              },
                              "timestamp": "2026-01-15T10:30:00Z"
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "The user referenced by the token no longer exists")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        var user = userRepository.findById(securityUtils.getCurrentUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", securityUtils.getCurrentUserId()));
        return ResponseUtil.ok(userMapper.toResponse(user));
    }
}
