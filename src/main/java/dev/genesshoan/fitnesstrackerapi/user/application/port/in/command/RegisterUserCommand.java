package dev.genesshoan.fitnesstrackerapi.user.application.port.in.command;

/**
 * Data required to create a new user account.
 *
 * @param email        the user's email address, must be unique
 * @param username     the user's username, must be unique
 * @param timezone     the user's preferred timezone
 * @param passwordHash the password, already encoded
 */
public record RegisterUserCommand(String email, String username, String timezone, String passwordHash) {
    @Override
    public String toString() {
        return "RegisterUserCommand[email=" + email + ", username=" + username + ", timezone=" + timezone
                + ", passwordHash=***]";
    }
}
