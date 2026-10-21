package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import dev.genesshoan.fitnesstrackerapi.auth.application.exception.InvalidJwtException;
import dev.genesshoan.fitnesstrackerapi.auth.application.result.TokenData;
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
    private final Clock clock;
    private SecretKey secretKey;

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
    public String buildAccessToken(UUID id, List<String> authorities) {
        Instant now = clock.instant();

        return Jwts.builder()
                .subject(id.toString())
                .claim("authorities", authorities)
                .issuer("FitnessTrackerApp")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.accessTokenExpiration())))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extracts all claims from a JWT after verifying its signature.
     *
     * @param token JWT string
     * @return parsed claims
     */
    public TokenData parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer("FitnessTrackerApp")
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String sub = claims.getSubject();
            if (sub == null) throw new InvalidJwtException("Missing subject");

            Date exp = claims.getExpiration();
            if (exp == null) throw new InvalidJwtException("Missing expiration");

            return new TokenData(
                    UUID.fromString(claims.getSubject()),
                    extractAuthorities(claims),
                    claims.getIssuedAt(),
                    claims.getExpiration());
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            throw new InvalidJwtException("Invalid or expired token", ex);
        }
    }

    private Collection<? extends GrantedAuthority> extractAuthorities(Claims claims) {
        Object value = claims.get("authorities");

        if (!(value instanceof List<?> roles) || !roles.stream().allMatch(String.class::isInstance)) {
            throw new InvalidJwtException("Invalid roles claim");
        }

        try {
            return roles.stream()
                    .map(String.class::cast)
                    .map(SimpleGrantedAuthority::new)
                    .toList();
        } catch (IllegalArgumentException ex) {
            throw new InvalidJwtException("Invalid roles claim", ex);
        }
    }
}
