package dev.genesshoan.fitnesstrackerapi.user.application.port.in;

import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.PendingVerificationUser;
import java.util.Optional;
import java.util.UUID;

/**
 * Marks a user's email address as verified.
 */
public interface VerifyUserEmailUseCase {

    /**
     * Finds a user whose email address has not been verified yet.
     *
     * <p>Returns an empty {@code Optional} both when no user has that email and when the user is already
     * verified, so callers cannot (and should not) distinguish between the two cases. This keeps the
     * resend-verification flow from revealing which emails are registered.
     *
     * @param email the email address to look up
     * @return the pending user, or empty if the email is unknown or already verified
     */
    Optional<PendingVerificationUser> findUnverifiedByEmail(String email);

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
