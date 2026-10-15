package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web;

import java.time.Duration;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.genesshoan.fitnesstrackerapi.auth.application.command.RegisterCommand;
import dev.genesshoan.fitnesstrackerapi.auth.application.service.RegistrationService;
import dev.genesshoan.fitnesstrackerapi.auth.application.service.SessionService;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.config.RefreshTokenProperties;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.LoginRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.MessageResponse;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.RegisterRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.ResendVerificationEmailRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.TokenResponse;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.VerifyEmailRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Authentication controller responsible for handling user authentication flows.
 *
 * <p>
 * Provides endpoints for:
 * <ul>
 * <li>User registration</li>
 * <li>User login</li>
 * <li>JWT refresh token rotation</li>
 * <li>User logout (refresh token revocation)</li>
 * </ul>
 *
 * <p>
 * This controller works with a stateless JWT authentication model:
 * <ul>
 * <li>Access tokens are short-lived and not stored server-side</li>
 * <li>Refresh tokens are persisted in the database and rotated</li>
 * <li>Logout revokes refresh tokens by family ID</li>
 * </ul>
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "JWT authentication, refresh token rotation, logout")
public class AuthController {

    private final SessionService sessionService;

    private final RegistrationService registrationService;

    private final RefreshTokenProperties refreshTokenProperties;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account, encodes password securely, and sends a verification email.",
            security = {})
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "User successfully created",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = MessageResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid request data",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class))),
                @ApiResponse(
                        responseCode = "409",
                        description = "User already exists",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest dto) {
        RegisterCommand command = new RegisterCommand(dto.username(), dto.password(), dto.email(), dto.timezone());

        registrationService.register(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Please check your email to verify your account"));
    }

    @Operation(
            summary = "Verify email address",
            description = "Verifies a user's email address using a verification token.",
            security = {})
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "Email address successfully verified"),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid request data or verification token",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @PostMapping("verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest dto) {
        registrationService.verifyEmail(dto.token());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Verify email address",
            description = "Verifies a user's email address using a verification token.",
            security = {})
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "Email address successfully verified"),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid request data or verification token",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @PostMapping("resend-verification")
    public ResponseEntity<Void> resendVerificationEmail(@Valid @RequestBody ResendVerificationEmailRequest dto) {
        registrationService.resendVerificationEmail(dto.email());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Authenticate user",
            description = "Validates credentials and returns JWT access and refresh tokens.",
            security = {})
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Authentication successful",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = TokenResponse.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Invalid credentials",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest dto) {
        var tokens = sessionService.login(dto.email(), dto.password());

        var cookie = createRefreshCookie(tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(tokens.accessToken()));
    }

    @Operation(
            summary = "Refresh JWT tokens",
            description = "Rotates refresh token and issues a new access + refresh token pair.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Token refreshed successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = TokenResponse.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Invalid or expired refresh token",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @Parameter(description = "Refresh token  cookie") @CookieValue("refresh_token") String refreshToken) {
        var tokens = sessionService.refreshToken(refreshToken);

        var cookie = createRefreshCookie(tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(tokens.accessToken()));
    }

    @Operation(
            summary = "Logout user",
            description =
                    "Revokes all refresh tokens belonging to the same token family. Access token remains valid until"
                            + " expiration.")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "Logout successful"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Invalid token",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @DeleteMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "Refresh token cookie") @CookieValue("refresh_token") String refreshToken) {
        sessionService.logout(refreshToken);

        var cookie = clearRefreshCookie();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(refreshTokenProperties.cookieSecure())
                .path("/api/v1/auth")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie createRefreshCookie(String refreshToken) {
        return ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(refreshTokenProperties.cookieSecure())
                .path("/api/v1/auth")
                .sameSite("Lax")
                .maxAge(refreshTokenProperties.expiration())
                .build();
    }
}
