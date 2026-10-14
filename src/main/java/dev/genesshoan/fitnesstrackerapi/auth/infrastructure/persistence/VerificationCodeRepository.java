package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationCode;
import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationPurpose;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

    @Query("""
        SELECT c FROM VerificationCode c
        WHERE c.subjectId = :subjectId
          AND c.purpose = :purpose
          AND c.usedAt IS NULL
          AND c.expiresAt > :now
        ORDER BY c.createdAt DESC
        LIMIT 1
        """)
    Optional<VerificationCode> findActive(
            @Param("subjectId") UUID subjectId,
            @Param("purpose") VerificationPurpose purpose,
            @Param("now") Instant now);

    Optional<VerificationCode> findByHashAndPurpose(byte[] hash, VerificationPurpose purpose);

    @Modifying
    @Query("""
        UPDATE VerificationCode c SET c.usedAt = :now
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
        UPDATE VerificationCode c SET c.usedAt = :now
        WHERE c.id = :id
          AND c.usedAt IS NULL
          AND c.expiresAt > :now
        """)
    int markUsed(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("""
        UPDATE VerificationCode c SET c.attempts = c.attempts + 1
        WHERE c.id = :id
          AND c.usedAt IS NULL
          AND c.expiresAt > :now
          AND c.attempts < :max
        """)
    int incrementAttempts(@Param("id") UUID id, @Param("now") Instant now, @Param("max") int max);

    @Modifying
    @Query("DELETE FROM VerificationCode c WHERE c.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
