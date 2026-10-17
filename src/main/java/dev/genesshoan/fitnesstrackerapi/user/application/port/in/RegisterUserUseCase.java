package dev.genesshoan.fitnesstrackerapi.user.application.port.in;

import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.command.RegisterUserCommand;

/**
 * Creates new user accounts.
 */
public interface RegisterUserUseCase {

    /**
     * Persists a new user with the default role.
     *
     * <p>The password must already be encoded by the caller. The role is decided
     * by the user module and cannot be chosen through the command.
     *
     * @param command the registration data, including the already encoded password hash
     * @return the token data of the newly created user
     * @throws ResourceAlreadyExistsException if the email or username is already registered
     */
    UUID register(RegisterUserCommand command);
}
