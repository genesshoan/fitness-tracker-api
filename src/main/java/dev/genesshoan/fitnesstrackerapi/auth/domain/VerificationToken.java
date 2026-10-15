package dev.genesshoan.fitnesstrackerapi.auth.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import dev.genesshoan.fitnesstrackerapi.common.domain.IdentifiableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Verification token entity for email verification and other verification purposes.
 *
 * <p>
 * Stores a hashed verification token associated with a user (subject) and a
 * specific verification purpose. Tokens expire after a configured duration
 * and can be consumed only once.
 *
 * <p>This entity manages verification tokens used for email confirmation,
 * password resets, and other security-sensitive operations. Tokens are stored
 * in hashed form and have a single-use constraint for security.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-5.1.1">JWT ID (jti)</a>
 * @see VerificationPurpose for supported verification purposes
 * @see dev.genesshoan.fitnesstrackerapi.auth.application.service.RegistrationService#verifyEmail(String)
 */
@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Table(name = "verification_tokens", indexes = @Index(columnList = "subject_id, purpose, expires_at"))
public class VerificationToken extends IdentifiableEntity {

    /**
     * The user ID (subject) that this verification token belongs to.
     *
     * <p>References the users table via a foreign key constraint with cascade
     * delete behavior. Identifies which user needs to perform the verification
     * action (e.g., email confirmation, password reset).
     *
     * @see dev.genesshoan.fitnesstrackerapi.user.domain.User
     */
    @Column(nullable = false, updatable = false)
    private UUID subjectId;

    /**
     * Cryptographically hashed version of the verification token.
     *
     * <p>Uses SHA-256 hashing for secure storage. The plain token value is never
     * persisted, preventing exposure even if the database is compromised.
     *
     * @see #getHash()
     */
    @Column(nullable = false, length = 32, updatable = false)
    private byte[] hash;

    /**
     * The purpose of this verification token.
     *
     * <p>Determines what action the token enables (e.g., EMAIL_VERIFICATION,
     * PASSWORD_RESET). This restricts token usage to specific security operations.
     *
     * @see VerificationPurpose
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationPurpose purpose;

    /**
     * Timestamp when the token was created.
     *
     * <p>Automatically populated by Hibernate at entity creation time.
     * This field is read-only and cannot be updated once set.
     *
     * @see #getCreatedAt()
     */
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the token expires and becomes invalid.
     *
     * <p>Tokens must be consumed before this timestamp. Once expired, the token
     * cannot be used for verification and is eligible for cleanup.
     *
     * @see #getExpiresAt()
     */
    @Column(updatable = false, nullable = false)
    private Instant expiresAt;

    /**
     * Timestamp when the token was successfully consumed/used.
     *
     * <p>Populated when the token is validated and the associated verification
     * action is completed (e.g., email marked as verified). Null until used.
     * This enforces the single-use constraint for security.
     *
     * @see #getUsedAt()
     */
    private Instant usedAt;
}
