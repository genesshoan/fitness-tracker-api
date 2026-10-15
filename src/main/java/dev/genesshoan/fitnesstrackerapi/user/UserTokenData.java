package dev.genesshoan.fitnesstrackerapi.user;

import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.user.domain.Role;

public record UserTokenData(UUID subjectId, String username, Role role, boolean emailVerified) {}
