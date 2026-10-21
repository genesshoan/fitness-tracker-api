package dev.genesshoan.fitnesstrackerapi.auth.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.auth.application.result.IssuedToken;
import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationPurpose;
import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationToken;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.config.VerificationProperties;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence.VerificationTokenRepository;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.security.OpaqueTokens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for issuing and managing verification codes.
 *
 * <p>
 * Handles the complete lifecycle of verification codes including:
 * <ul>
 * <li>Generating cryptographically secure random codes</li>
 * <li>Creating verification code records in the database</li>
 * <li>Invalidating existing active codes for the same subject and purpose</li>
 * <li>Validating and consuming codes upon successful verification</li>
 * <li>Tracking verification attempts and enforcing attempt limits</li>
 * </ul>
 *
 * <p>
 * Uses SHA-256 hashing to store verification codes securely, preventing
 * storage of plain-text codes and protecting against unauthorized code recovery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationProperties properties;
    private final VerificationTokenRepository repository;
    private final Clock clock;

    /**
     * Generates and persists a verification token for the specified subject and purpose.
     *
     * <p>
     * First invalidates any existing active tokens for the same subject and purpose,
     * then generates a cryptographically secure token, creates a verification code
     * record in the database, and returns the token with its expiration time.
     *
     * <p>The expiration is resolved from the purpose, so each kind of token
     * (email verification, password reset) can have its own lifetime.
     *
     * @param subjectId the user identifier receiving the verification
     * @param purpose   the verification purpose (e.g. email verification)
     * @return issued token details including the token string and expiration
     */
    @Transactional
    public IssuedToken issue(UUID subjectId, VerificationPurpose purpose) {
        log.info("Issuing verification token for subject={} purpose={}", subjectId, purpose);
        Instant now = clock.instant();
        repository.invalidateActive(subjectId, purpose, now);

        String token = OpaqueTokens.generate();
        Instant expiresAt = now.plus(expirationFor(purpose));

        repository.save(VerificationToken.builder()
                .subjectId(subjectId)
                .purpose(purpose)
                .hash(OpaqueTokens.sha256(token))
                .createdAt(now)
                .expiresAt(expiresAt)
                .build());

        log.info("Verification token issued for subject={}. Expires at {}", subjectId, expiresAt);
        return new IssuedToken(token, expiresAt);
    }

    /**
     * Validates and consumes a verification token.
     *
     * <p>
     * <b>Returns:</b> <b>Optional</b> containing the subject ID if the token
     * is valid and not yet used, or <b>Optional.empty()</b> if the token is
     * invalid, expired, or already consumed.
     *
     * <p>
     * This method verifies the token hash against stored hashes and marks
     * the token as used upon successful validation (single-use semantics).
     *
     * @param purpose the verification purpose to validate against
     * @param token   the raw verification token string
     * @return optional subject ID if token is valid and unconsumed
     */
    @Transactional
    public Optional<UUID> consume(VerificationPurpose purpose, String token) {
        log.info("Consuming verification token for purpose={}", purpose);
        byte[] hash = OpaqueTokens.sha256(token);

        var found = repository.findByHashAndPurpose(hash, purpose);

        if (found.isEmpty() || repository.markUsed(found.get().getId(), clock.instant()) == 0) {
            return Optional.empty();
        }

        log.info(
                "Verification token consumed successfully for subject={}",
                found.get().getSubjectId());
        return Optional.of(found.get().getSubjectId());
    }

    /**
     * Resolves the lifetime of a token based on its purpose.
     *
     * @param purpose the verification purpose
     * @return the configured expiration for the purpose
     */
    private Duration expirationFor(VerificationPurpose purpose) {
        return switch (purpose) {
            case EMAIL_VERIFICATION -> properties.emailVerificationExpiration();
            case PASSWORD_RESET -> properties.passwordResetExpiration();
        };
    }
}
