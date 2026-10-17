package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import dev.genesshoan.fitnesstrackerapi.auth.application.exception.InvalidJwtException;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

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
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;
    private Clock clock;

    /**
     * Initializes the HMAC signing key from a Base64-encoded secret.
     * This method is executed once after dependency injection.
     */
    @PostConstruct
    private void init() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.secret());
        secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Builds an access token containing username and roles.
     */
    public String buildAccessToken(String username, List<String> authorities) {
        Instant now = clock.instant();

        return Jwts.builder()
                .subject(username)
                .claim("authorities", authorities)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.accessTokenExpiration())))
                .signWith(secretKey)
                .compact();
    }

    private String require(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new InvalidJwtException(errorMessage);
        }
        return value;
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
        return require(parseToken(token).getSubject(), "Missing subject");
    }
}
