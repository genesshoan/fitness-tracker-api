package dev.genesshoan.fitnesstrackerapi.auth.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import com.github.f4b6a3.uuid.UuidCreator;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistent representation of JWT refresh tokens.
 *
 * Each refresh token is uniquely identified by its JWT ID (jti)
 * and grouped into a session family (familyId) for revocation and rotation.
 * The user relationship is lazy-loaded.
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tokens")
public class Token {

    /**
     * Unique JWT ID for this token instance.
     * Used for database tracking, revocation, and reuse detection.
     */
    @Id
    @Column(updatable = false, nullable = false, unique = true)
    @Builder.Default
    private UUID jti = UuidCreator.getTimeOrderedEpoch();

    /**
     * Session family identifier. Groups all refresh tokens belonging
     * to the same login session for batch revocation.
     */
    @Column(nullable = false)
    private UUID familyId;

    /**
     * Revocation status. {@code true} when the token has been
     * invalidated (logged out or detected as reused).
     */
    @Builder.Default
    private boolean revoked = false;

    /**
     * Expiration instant for this token.
     */
    @Column(nullable = false)
    private Instant expiresAt;

    /**
     * The user to whom this refresh token belongs.
     * Lazy-loaded; must be initialized within an active persistence context.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
