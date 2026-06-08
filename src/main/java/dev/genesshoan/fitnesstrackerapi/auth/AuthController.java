package dev.genesshoan.fitnesstrackerapi.auth;

import org.springframework.http.HttpStatus;
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
public class AuthController {

  /**
   * Service that handles authentication logic, including:
   * token generation, validation, rotation, and revocation.
   */
  private final AuthService authService;

  /**
   * Registers a new user and returns a JWT token pair.
   *
   * <p>
   * On success:
   * <ul>
   * <li>Creates a new user in the system</li>
   * <li>Encodes password securely</li>
   * <li>Issues access token and refresh token</li>
   * </ul>
   *
   * @param dto registration request containing user credentials
   * @return HTTP 201 (CREATED) with JWT token pair
   */
  @PostMapping("/register")
  public ResponseEntity<TokenResponseDTO> register(
      @Valid @RequestBody RegisterRequestDTO dto) {

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(authService.register(dto));
  }

  /**
   * Authenticates a user and returns a JWT token pair.
   *
   * <p>
   * On success:
   * <ul>
   * <li>Validates user credentials</li>
   * <li>Generates access token</li>
   * <li>Generates refresh token (stored in DB)</li>
   * </ul>
   *
   * @param dto login request containing email and password
   * @return HTTP 200 (OK) with JWT token pair
   */
  @PostMapping("/login")
  public ResponseEntity<TokenResponseDTO> login(
      @Valid @RequestBody LoginRequestDTO dto) {

    return ResponseEntity
        .ok(authService.login(dto));
  }

  /**
   * Rotates refresh token and issues a new token pair.
   *
   * <p>
   * Flow:
   * <ul>
   * <li>Extracts Bearer token from Authorization header</li>
   * <li>Validates refresh token integrity</li>
   * <li>Checks database for revocation status</li>
   * <li>Detects token reuse and revokes entire family if needed</li>
   * <li>Generates new access and refresh tokens</li>
   * </ul>
   *
   * @param request HTTP request containing Authorization header
   * @return HTTP 200 (OK) with new token pair
   */
  @PostMapping("/refresh")
  public ResponseEntity<TokenResponseDTO> refresh(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

    return ResponseEntity.ok(authService.refreshToken(authHeader));
  }

  /**
   * Logs out the user by revoking all refresh tokens belonging to the same
   * family.
   *
   * <p>
   * Flow:
   * <ul>
   * <li>Extracts Bearer token from Authorization header</li>
   * <li>Identifies token family</li>
   * <li>Revokes all tokens in that family</li>
   * </ul>
   *
   * <p>
   * Access tokens remain valid until expiration (stateless nature).
   *
   * @param request HTTP request containing Authorization header
   * @return HTTP 204 (NO CONTENT)
   */
  @DeleteMapping("/logout")
  public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

    authService.logout(authHeader);

    return ResponseEntity.noContent().build();
  }
}
