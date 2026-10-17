package dev.genesshoan.fitnesstrackerapi.user.application.port.in;

import java.util.UUID;

/**
 * Marks a user's email address as verified.
 */
public interface VerifyUserEmailUseCase {

    /**
     * Marks the user's email address as verified.
     *
     * <p>Calling it on an already verified user is a no-op that still returns {@code true}.
     *
     * @param id the user id
     * @return {@code true} if the user exists, {@code false} otherwise
     */
    boolean markEmailVerified(UUID id);
}
