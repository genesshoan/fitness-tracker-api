package dev.genesshoan.fitnesstrackerapi.auth;

import dev.genesshoan.fitnesstrackerapi.auth.domain.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TokenRepository extends JpaRepository<Token, UUID> {

  boolean existsByJtiAndRevokedFalse(UUID jti);

  @Modifying
  @Query("""
        UPDATE Token t
        SET t.revoked = true
        WHERE t.familyId = :familyId
          AND t.revoked = false
      """)
  void revokeByFamily(@Param("familyId") UUID familyId);
}
