package dev.genesshoan.fitnesstrackerapi.auth.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import dev.genesshoan.fitnesstrackerapi.common.domain.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Verification code entity for email verification and other verification purposes.
 *
 * <p>
 * Stores a hashed verification code associated with a user (subject) and a
 * specific verification purpose. Codes expire after a configured duration
 * and can be consumed only once. Failed attempts are tracked to prevent
 * brute-force attacks.
 */
@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Table(name = "verification_codes", indexes = @Index(columnList = "user_id, type, expires_at"))
public class VerificationCode extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID subjectId;

    @Column(name = "hash", nullable = false, length = 32, updatable = false)
    private byte[] hash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationPurpose purpose;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(updatable = false, nullable = false)
    private Instant expiresAt;

    private Instant usedAt;
}
