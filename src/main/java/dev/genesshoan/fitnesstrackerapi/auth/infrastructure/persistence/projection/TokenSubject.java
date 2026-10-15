package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence.projection;

import java.time.Instant;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.user.domain.Role;

public interface TokenSubject {

    String getUsername();

    UUID getTokenId();

    UUID getSubjectId();

    Role getRole();

    UUID getTokenFamilyId();

    boolean getRevoked();

    Instant getExpiresAt();
}
