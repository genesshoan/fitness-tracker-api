package dev.genesshoan.fitnesstrackerapi.auth;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.genesshoan.fitnesstrackerapi.auth.domain.Token;

/**
 * Repository for persistent refresh token management.
 *
 * Provides operations to find, create, and revoke refresh tokens.
 * Token revocation supports family-level revocation for session management
 * and token reuse detection.
 */
public interface TokenRepository extends JpaRepository<Token, UUID> {

    /**
     * Checks whether a token with the given JTI exists and is not revoked.
     *
     * @param jti the JWT ID to check
     * @return true if a non-revoked token with this JTI exists
     */
    boolean existsByJtiAndRevokedFalse(UUID jti);

    /**
     * Revokes all non-revoked refresh tokens belonging to the given family ID.
     *
     * <p>This method is used during logout and token reuse detection.
     * Only tokens with {@code revoked = false} are updated to {@code revoked = true}.
     * Already-revoked tokens are left unchanged.</p>
     *
     * @param familyId the token family identifier; all tokens in this family
     *                 with {@code revoked = false} will be revoked
     */
    @Modifying
    @Query("""
        UPDATE Token t
        SET t.revoked = true
        WHERE t.familyId = :familyId
          AND t.revoked = false
      """)
    void revokeByFamily(@Param("familyId") UUID familyId);
}
