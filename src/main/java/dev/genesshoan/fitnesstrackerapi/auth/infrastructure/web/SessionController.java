package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.genesshoan.fitnesstrackerapi.auth.application.service.SessionService;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.LoginRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.TokenResponse;
import dev.genesshoan.fitnesstrackerapi.ratelimit.domain.RateLimitPolicy;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.RateLimited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Handles the session lifecycle: login, refresh token rotation and logout.
 *
 * <p>Access tokens are short-lived JWTs that are not stored server-side. Refresh
 * tokens are opaque, persisted in the database, rotated on every use, and sent
 * to the client in an HttpOnly cookie.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Session", description = "Login, refresh token rotation and logout")
public class SessionController {

    private final SessionService sessionService;
    private final RefreshCookieFactory refreshCookieFactory;

    @Operation(
            summary = "Authenticate user",
            description =
                    "Validates credentials and returns an access token in the body and a refresh token in an HttpOnly"
                            + " cookie.",
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
                        responseCode = "400",
                        description = "Invalid request data",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Invalid email or password",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class))),
                @ApiResponse(
                        responseCode = "403",
                        description = "Email address not verified",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class))),
                @ApiResponse(
                        responseCode = "429",
                        description = "Too many login attempts for this IP address or account",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @PostMapping("/login")
    @RateLimited(policy = RateLimitPolicy.LOGIN, accountKey = "#dto.email")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest dto) {
        var tokens = sessionService.login(dto.email(), dto.password());

        var cookie = refreshCookieFactory.create(tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(tokens.accessToken()));
    }

    @Operation(
            summary = "Refresh tokens",
            description = "Rotates the refresh token cookie and issues a new access token. Reusing an already rotated"
                    + " token revokes the whole token family.",
            security = {})
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Tokens refreshed successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = TokenResponse.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Refresh token invalid, expired, revoked or reused",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @Parameter(description = "Refresh token cookie") @CookieValue("refresh_token") String refreshToken) {
        var tokens = sessionService.refreshToken(refreshToken);

        var cookie = refreshCookieFactory.create(tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(tokens.accessToken()));
    }

    @Operation(
            summary = "Logout user",
            description = "Revokes the refresh token and clears the cookie. The access token remains valid until it"
                    + " expires.",
            security = {})
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "Logout successful"),
                @ApiResponse(
                        responseCode = "400",
                        description = "Refresh token cookie is missing",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @DeleteMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "Refresh token cookie") @CookieValue("refresh_token") String refreshToken) {
        sessionService.logout(refreshToken);

        var cookie = refreshCookieFactory.clear();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}
