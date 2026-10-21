package dev.genesshoan.fitnesstrackerapi.auth.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import dev.genesshoan.fitnesstrackerapi.common.domain.IdentifiableEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistent representation of JWT refresh tokens.
 *
 * <p>Each refresh token is uniquely identified by its JWT ID (jti)
 * and grouped into a session family (familyId) for revocation and rotation.
 * The user relationship is lazy-loaded.
 *
 * <p>This entity stores opaque refresh tokens in hashed form for security,
 * supporting token rotation and family-based revocation for logout.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-5.1.1">JWT ID (jti)</a>
 * @see {@link dev.genesshoan.fitnesstrackerapi.auth.application.service.SessionService#refreshToken(String)}
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "refresh_tokens",
        indexes = {@Index(columnList = "family_id"), @Index(columnList = "user_id")})
public class RefreshToken extends IdentifiableEntity {

    /**
     * Cryptographically hashed version of the refresh token for secure storage.
     *
     * <p>Uses SHA-256 hashing to prevent token leakage if the database is compromised.
     * The hash is stored instead of the plain token value.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-5.1.1">JWT ID (jti)</a>
     */
    @Column(nullable = false, updatable = false, unique = true, length = 32)
    private byte[] hash;

    /**
     * Identifier for grouping refresh tokens into a session family.
     *
     * <p>All refresh tokens issued for the same user session share the same
     * family ID. This enables family-wide revocation (e.g., on logout or
     * security events) and ensures token rotation without breaking sessions.
     *
     * @see #revokeByFamily(UUID)
     */
    @Column(nullable = false)
    private UUID familyId;

    /**
     * Indicates whether the token has been revoked.
     *
     * <p>Tokens are marked as revoked when they are consumed during token refresh
     * or when the entire session is revoked (e.g., on logout or security events).
     * Revoked tokens are excluded from future refresh operations.
     *
     * @see #getRevoked()
     */
    @Builder.Default
    private boolean revoked = false;

    /**
     * Timestamp when the token expires and becomes invalid.
     *
     * <p>Tokens must be used before this timestamp expires. Once expired,
     * the token cannot be used for refreshing access tokens.
     *
     * @see #getExpiresAt()
     */
    @Column(nullable = false)
    private Instant expiresAt;

    /**
     * The user ID that this refresh token belongs to.
     *
     * <p>Stored as a plain identifier instead of an entity association, so the token
     * can be read without loading the user.
     */
    @Column(nullable = false)
    private UUID userId;
}
