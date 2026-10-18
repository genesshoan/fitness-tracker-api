package dev.genesshoan.fitnesstrackerapi.user.application.port.in;

import java.util.Optional;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.UserCredentials;

/**
 * Provides a user's stored credentials for authentication flows.
 *
 * <p>The user module only stores the password hash. Hashing, matching and
 * password policies belong to the authentication module.
 */
public interface GetUserCredentialsUseCase {

    /**
     * Finds a user's credentials by email.
     *
     * @param email the user's email address
     * @return the user's credentials, or empty if no user has that email
     */
    Optional<UserCredentials> findByEmail(String email);

    /**
     * Finds a user's credentials by email.
     *
     * @param email the user's email address
     * @return the user's credentials, or empty if no user has that email
     */
    Optional<UserCredentials> findByIdentifier(String identifier);

    /**
     * Finds the credentials of a user.
     *
     * @param id the user id
     * @return the credentials of a user, or empty if the user does not exist
     */
    Optional<UserCredentials> findById(UUID userId);
}
