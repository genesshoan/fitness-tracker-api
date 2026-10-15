package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationPurpose;
import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationToken;

/**
 * Spring Data JPA repository for {@link VerificationToken} entities.
 *
 * <p>Provides data access operations for verification token management,
 * including token lookup by hash, invalidation of active tokens,
 * marking tokens as used, and cleanup of expired tokens.
 *
 * @see VerificationToken
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    /**
     * Finds a verification token by its hash and purpose.
     *
     * <p>The hash is computed using {@link OpaqueTokens#sha256(String)} from the
     * raw token string, enabling secure lookup without storing the plain token.
     *
     * @param hash the SHA-256 hash of the token string
     * @param purpose the purpose of the token to match against
     * @return the verification token if found
     */
    Optional<VerificationToken> findByHashAndPurpose(byte[] hash, VerificationPurpose purpose);

    @Modifying
    @Query("""
        UPDATE VerificationToken c SET c.usedAt = :now
        WHERE c.subjectId = :subjectId
          AND c.purpose = :purpose
          AND c.usedAt IS NULL
        """)
    int invalidateActive(
            @Param("subjectId") UUID subjectId,
            @Param("purpose") VerificationPurpose purpose,
            @Param("now") Instant now);

    @Modifying
    @Query("""
        UPDATE VerificationToken c SET c.usedAt = :now
        WHERE c.id = :id
          AND c.usedAt IS NULL
          AND c.expiresAt > :now
        """)
    int markUsed(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM VerificationToken c WHERE c.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
