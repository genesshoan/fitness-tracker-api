package dev.genesshoan.fitnesstrackerapi.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;

import dev.genesshoan.fitnesstrackerapi.auth.dto.LoginRequestDTO;
import dev.genesshoan.fitnesstrackerapi.auth.dto.RegisterRequestDTO;
import dev.genesshoan.fitnesstrackerapi.auth.dto.TokenResponseDTO;
import dev.genesshoan.fitnesstrackerapi.auth.service.AuthService;
import jakarta.validation.Valid;
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

  /**
   * Service that handles authentication logic, including:
   * token generation, validation, rotation, and revocation.
   */
  private final AuthService authService;

  @Operation(summary = "Register a new user", description = "Creates a new user account, encodes password securely, and returns JWT access and refresh tokens.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "User successfully created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponseDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
      @ApiResponse(responseCode = "409", description = "User already exists", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PostMapping("/register")
  public ResponseEntity<TokenResponseDTO> register(
      @Valid @RequestBody RegisterRequestDTO dto) {

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(authService.register(dto));
  }

  @Operation(summary = "Authenticate user", description = "Validates credentials and returns JWT access and refresh tokens.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PostMapping("/login")
  public ResponseEntity<TokenResponseDTO> login(
      @Valid @RequestBody LoginRequestDTO dto) {

    return ResponseEntity.ok(authService.login(dto));
  }

  @Operation(summary = "Refresh JWT tokens", description = "Rotates refresh token and issues a new access + refresh token pair.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Token refreshed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PostMapping("/refresh")
  public ResponseEntity<TokenResponseDTO> refresh(
      @Parameter(description = "Bearer refresh token") @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

    return ResponseEntity.ok(authService.refreshToken(authHeader));
  }

  @Operation(summary = "Logout user", description = "Revokes all refresh tokens belonging to the same token family. Access token remains valid until expiration.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Logout successful"),
      @ApiResponse(responseCode = "401", description = "Invalid token", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
  })
  @DeleteMapping("/logout")
  public ResponseEntity<Void> logout(
      @Parameter(description = "Bearer access token") @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

    authService.logout(authHeader);
    return ResponseEntity.noContent().build();
  }
}
