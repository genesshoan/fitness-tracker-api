package dev.genesshoan.fitnesstrackerapi.user.application.port.in;

import java.util.Optional;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.UserTokenData;

/**
 * Read-only view of the user data that other modules need in order to issue tokens.
 *
 * <p>Implemented by the user module and consumed by the authentication module,
 * so the latter never depends on the user entity or repository.
 */
public interface GetUserTokenDataUseCase {

    /**
     * Finds the token data of a user by email.
     *
     * @param email the user's email address
     * @return the user's token data, or empty if no user has that email
     */
    Optional<UserTokenData> findByEmail(String email);

    /**
     * Finds the token data of a user by id.
     *
     * @param id the user id
     * @return the user's token data, or empty if the user does not exist
     * @throws ResourceNotFoundException if the user does not exist
     */
    Optional<UserTokenData> findById(UUID id);
}
