package dev.genesshoan.fitnesstrackerapi.user.application.port.in;

import java.util.Optional;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.RefreshUserData;

/**
 * Read-only view of the user data that other modules need in order to issue tokens.
 *
 * <p>Implemented by the user module and consumed by the authentication module,
 * so the latter never depends on the user entity or repository.
 */
public interface GetRefreshUserDataUseCase {

    /**
     * Finds the data needed to issue a new access token during a refresh.
     *
     * <p>Reads the current state from the database, so changes made after the previous token was
     * issued (such as a role change) are reflected in the new token.
     *
     * @param id the user id taken from the refresh token
     * @return the user's refresh data, or empty if the user no longer exists
     */
    Optional<RefreshUserData> findRefreshDataById(UUID id);
}
