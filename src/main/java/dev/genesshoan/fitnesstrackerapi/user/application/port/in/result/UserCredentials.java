package dev.genesshoan.fitnesstrackerapi.user.application.port.in.result;

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
public record UserCredentials(String username, String email, String passwordHash, Role role) {
    @Override
    public String toString() {
        return "UserCredentials[email=" + email + ", passwordHash=***, role=" + role + "]";
    }
}
