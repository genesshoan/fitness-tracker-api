package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.genesshoan.fitnesstrackerapi.auth.domain.RefreshToken;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence.projection.TokenSubject;

/**
 * Repository for persistent refresh token management.
 *
 * Provides operations to find, create, and revoke refresh tokens.
 * Token revocation supports family-level revocation for session management
 * and token reuse detection.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Checks whether a token with the given ID exists and is not revoked.
     *
     * @param jti the JWT ID to check
     * @return true if a non-revoked token with this ID exists
     */
    boolean existsByIdAndRevokedFalse(UUID id);

    @Query("""
            SELECT
                u.username
                r.id AS tokenId,
                u.id AS subjectId,
                u.role
                r.familyId AS tokenFamilyId,
                r.revoked
                r.expiresAt
            FROM RefreshToken r
            JOIN User u ON u.id = r.userId
            WHERE r.hash = :hash
        """)
    Optional<TokenSubject> findByHash(@Param("hash") byte[] hash);

    @Modifying
    @Query("""
            UPDATE RefreshToken t
            SET t.revoked = true
            WHERE t.id = :id
            AND t.revoked = false
        """)
    int revokeById(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            UPDATE refresh_tokens
            SET revoked = true
            WHERE familyId = (
                    SELECT family_id
                    FROM refresh_tokens
                    WHERE hash = :hash
                )
                AND revoked = false
        """, nativeQuery = true)
    int revokeByHash(@Param("hash") byte[] hash);

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
            UPDATE RefreshToken t
            SET t.revoked = true
            WHERE t.familyId = :familyId
                AND t.revoked = false
        """)
    void revokeByFamily(@Param("familyId") UUID familyId);
}
