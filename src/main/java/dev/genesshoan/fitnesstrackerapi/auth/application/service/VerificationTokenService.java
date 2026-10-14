package dev.genesshoan.fitnesstrackerapi.auth.application.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.auth.application.result.IssuedToken;
import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationCode;
import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationPurpose;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.config.VerificationProperties;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence.VerificationCodeRepository;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.security.CodeHasher;
import lombok.RequiredArgsConstructor;

/**
 * Service responsible for issuing and managing verification tokens for email verification.
 *
 * <p>
 * Handles the complete lifecycle of verification tokens including:
 * <ul>
 * <li>Generating secure, random token strings</li>
 * <li>Creating verification code records in the database</li>
 * <li>Invalidating existing active tokens for the same user and purpose</li>
 * <li>Validating and consuming tokens for email verification</li>
 * </ul>
 *
 * <p>
 * Uses HMAC-based hashing to store verification codes securely, preventing
 * storage of plain text codes and providing tamper protection.
 */
@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final VerificationProperties properties;
    private final CodeHasher codeHasher;
    private final VerificationCodeRepository repository;
    private final Clock clock;

    /**
     * Generates and persists a verification token for the specified subject and purpose.
     *
     * <p>
     * First invalidates any existing active tokens for the same subject and purpose,
     * then generates a cryptographically secure token, creates a verification code
     * record in the database, and returns the token with its expiration time.
     *
     * @param subjectId the user identifier receiving the verification
     * @param purpose   the verification purpose (e.g. email verification)
     * @return issued token details including the token string and expiration
     */
    @Transactional
    public IssuedToken issue(UUID subjectId, VerificationPurpose purpose) {
        Instant now = clock.instant();
        repository.invalidateActive(subjectId, purpose, now);

        String token = generate();
        Instant expiresAt = now.plus(properties.tokenExpiration());

        repository.save(VerificationCode.builder()
                .subjectId(subjectId)
                .purpose(purpose)
                .hash(codeHasher.hashToken(purpose, token))
                .createdAt(now)
                .expiresAt(expiresAt)
                .build());

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
        byte[] hash = codeHasher.hashToken(purpose, token);

        var found = repository.findByHashAndPurpose(hash, purpose);

        if (found.isEmpty() || repository.markUsed(found.get().getId(), clock.instant()) == 0) {
            return Optional.empty();
        }

        return Optional.of(found.get().getSubjectId());
    }

    private String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
