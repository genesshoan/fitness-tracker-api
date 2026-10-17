package dev.genesshoan.fitnesstrackerapi.user.application.port.in.result;

import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.user.domain.Role;

/**
 * Minimal snapshot of a user, containing what is needed to issue tokens.
 *
 * @param subjectId     the user id, used as the token subject
 * @param username      the user's current username
 * @param role          the user's current role
 * @param emailVerified whether the user has verified their email address
 */
public record UserTokenData(UUID subjectId, String username, Role role, boolean emailVerified) {}
