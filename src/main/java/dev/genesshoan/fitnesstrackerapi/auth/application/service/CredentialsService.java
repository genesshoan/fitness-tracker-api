package dev.genesshoan.fitnesstrackerapi.auth.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.auth.application.event.PasswordChanged;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.IncorrectPasswordException;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.InvalidCredentialsException;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence.RefreshTokenRepository;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.GetUserCredentialsUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.UpdatePasswordHashUseCase;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CredentialsService {

    private final GetUserCredentialsUseCase getUserCredentialsUseCase;
    private final UpdatePasswordHashUseCase updatePasswordHashUseCase;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Changes the user's password.
     *
     * @param userId  the user id
     * @param oldPassword the user's current password
     * @param newPassword the password to replace
     * @throws ResourceNotFoundException   if the user does not exist
     * @throws InvalidCredentialsException if the provided password is incorrect
     * @throws BadRequestException         if the new password matches the current password
     */
    @Transactional
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        Instant now = clock.instant();

        var userCredentials = getUserCredentialsUseCase.findById(userId).orElseThrow(ResourceNotFoundException::new);

        if (!passwordEncoder.matches(oldPassword, userCredentials.passwordHash())) {
            throw new IncorrectPasswordException("Provided password does not match current one");
        }

        if (passwordEncoder.matches(newPassword, userCredentials.passwordHash())) {
            throw new BadRequestException("New password must be different from current one");
        }

        updatePasswordHashUseCase.updatePasswordHash(userId, passwordEncoder.encode(newPassword));

        refreshTokenRepository.revokeByUserId(userId);

        eventPublisher.publishEvent(new PasswordChanged(userCredentials.username(), userCredentials.email(), now));
    }
}
