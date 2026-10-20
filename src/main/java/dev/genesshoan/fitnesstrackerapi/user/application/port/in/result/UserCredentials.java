package dev.genesshoan.fitnesstrackerapi.user.application.port.in.result;

import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.user.domain.Role;

/**
 * The data the authentication module needs to verify a login attempt.
 *
 * <p>The hash must never be logged or exposed in any response.
 *
 * @param email        the user's email address, used as the login identifier
 * @param passwordHash the encoded password hash
 * @param role         the user's role, used to build the granted authorities
 */
public record UserCredentials(
        UUID id, String email, String username, String passwordHash, boolean emailVerified, Role role) {

    @Override
    public String toString() {
        return "UserCredentials[id=" + id
                + ", username=" + username
                + ", email=***"
                + ", passwordHash=***"
                + ", emailVerified=" + emailVerified
                + ", role=" + role + "]";
    }
}
