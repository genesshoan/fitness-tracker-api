package dev.genesshoan.fitnesstrackerapi.auth.service;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.InvalidJwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

/**
 * Service responsible for generating, signing, parsing and validating JWT
 * tokens.
 *
 * This service handles two types of tokens:
 *
 * 1. Access Token:
 * - Short-lived token used for authentication and authorization
 * - Contains user identity and roles/authorities
 * - Does NOT require persistence in the database
 *
 * 2. Refresh Token:
 * - Long-lived token used to obtain new access tokens
 * - Persisted in the database for revocation and session control
 * - Includes a unique identifier (jti) and a session identifier (familyId)
 *
 * Security model:
 * - Access tokens are stateless and validated using signature and expiration
 * only
 * - Refresh tokens are stateful and must be validated against stored records
 * - Token rotation is expected: each refresh produces a new refresh token
 * and invalidates the previous one
 *
 * The signing key is loaded from configuration and decoded from Base64
 * during application startup.
 */
@Service
public class JwtService {

  @Value("${application.security.jwt.secret}")
  private String secretKeyBase64;

  @Value("${application.security.jwt.expiration}")
  private long jwtExpiration;

  @Value("${application.security.jwt.refresh-token.expiration}")
  private long refreshExpiration;

  private SecretKey secretKey;

  /**
   * Initializes the HMAC signing key from a Base64-encoded secret.
   * This method is executed once after dependency injection.
   */
  @PostConstruct
  private void init() {
    byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
    secretKey = Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * Generates a signed JWT access token containing user identity and roles.
   *
   * @param userDetails authenticated user principal
   * @return signed JWT access token
   */
  public String generateToken(UserDetails userDetails) {
    return buildAccessToken(userDetails);
  }

  /**
   * Generates a signed JWT refresh token used for session continuation.
   *
   * @param userDetails authenticated user principal
   * @param jti         unique token identifier (used for DB tracking and reuse
   *                    detection)
   * @param familyId    session identifier grouping multiple refresh tokens
   * @return signed JWT refresh token
   */
  public String generateRefreshToken(UserDetails userDetails, UUID jti, UUID familyId) {
    return buildRefreshToken(userDetails, jti, familyId);
  }

  /**
   * Builds an access token containing username and roles.
   */
  private String buildAccessToken(UserDetails userDetails) {
    Instant now = Instant.now();

    List<String> roles = userDetails.getAuthorities()
        .stream()
        .map(GrantedAuthority::getAuthority)
        .toList();

    return Jwts.builder()
        .subject(userDetails.getUsername())
        .claim("roles", roles)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(jwtExpiration)))
        .signWith(secretKey)
        .compact();
  }

  /**
   * Builds a refresh token containing minimal identity information plus
   * session tracking metadata (jti and familyId).
   */
  private String buildRefreshToken(UserDetails userDetails, UUID jti, UUID familyId) {
    Instant now = Instant.now();

    return Jwts.builder()
        .subject(userDetails.getUsername())
        .id(jti.toString())
        .claim("familyId", familyId.toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(refreshExpiration)))
        .signWith(secretKey)
        .compact();
  }

  private String require(String value, String errorMessage) {
    if (value == null || value.isBlank()) {
      throw new InvalidJwtException(errorMessage);
    }
    return value;
  }

  private UUID requireUuid(String value, String errorMessage) {
    String v = require(value, errorMessage);

    try {
      return UUID.fromString(v);
    } catch (IllegalArgumentException ex) {
      throw new InvalidJwtException(errorMessage, ex);
    }
  }

  /**
   * Extracts all claims from a JWT after verifying its signature.
   *
   * @param token JWT string
   * @return parsed claims
   */
  private Claims parseToken(String token) {
    try {
      return Jwts.parser()
              .verifyWith(secretKey)
              .build()
              .parseSignedClaims(token)
              .getPayload();
    } catch (JwtException ex) {
      throw new InvalidJwtException("Invalid or expired token", ex);
    }
  }

  /**
   * Extracts the username (subject) from the JWT.
   * <p>
   * This value is required for authentication context.
   * If missing, the token is considered invalid.
   */
  public String extractUsername(String token) {
    return require(
            parseToken(token).getSubject(),
            "Missing subject"
    );
  }

  /**
   * Extracts the JWT ID (jti) claim.
   * <p>
   * The jti uniquely identifies a token instance and is required
   * for revocation, rotation, and reuse detection.
   */
  public UUID extractJti(String token) {
    return requireUuid(
            parseToken(token).getId(),
            "Missing or invalid jti"
    );
  }

  /**
   * Extracts the refresh token family identifier.
   * <p>
   * The family id is used to group refresh tokens belonging
   * to the same session chain (rotation tracking).
   */
  public UUID extractFamilyId(String token) {
    return requireUuid(
            parseToken(token).get("familyId", String.class),
            "Missing or invalid familyId"
    );
  }

  /**
   * Calculates the expiration instant for a refresh token based on the
   * configured refresh token lifetime.
   *
   * This method ensures that the expiration used in the database is consistent
   * with the expiration embedded in the JWT.
   *
   * @return Instant representing the refresh token expiration time
   */
  public Instant getRefreshTokenExpirationInstant() {
    return Instant.now().plusMillis(refreshExpiration);
  }
}
