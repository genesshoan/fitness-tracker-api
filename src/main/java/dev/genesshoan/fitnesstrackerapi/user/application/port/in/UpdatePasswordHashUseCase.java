package dev.genesshoan.fitnesstrackerapi.user.application.port.in;

import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;

/**
 * Replaces the password hash stored for a user.
 */
public interface UpdatePasswordHashUseCase {

    /**
     * Replaces the stored password hash of a user.
     *
     * <p>The hash must already be encoded. The previous password is not validated
     * here; that is the caller's responsibility.
     *
     * @param id      the user id
     * @param newHash the new, already encoded, password hash
     * @throws ResourceNotFoundException if the user does not exist
     */
    void updatePasswordHash(UUID id, String newHash);
}
