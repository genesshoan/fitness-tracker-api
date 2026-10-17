package dev.genesshoan.fitnesstrackerapi.auth.application.service;

import dev.genesshoan.fitnesstrackerapi.auth.application.event.PasswordChanged;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.IncorrectPasswordException;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence.RefreshTokenRepository;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.GetUserCredentialsUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.UpdatePasswordHashUseCase;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
     * @throws IncorrectPasswordException if the provided password is incorrect
     * @throws BadRequestException         if the new password matches the current password
     */
    @Transactional
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        log.info("Changing password for user id: {}", userId);
        Instant now = clock.instant();

        var userCredentials = getUserCredentialsUseCase.findById(userId).orElseThrow(ResourceNotFoundException::new);

        if (!passwordEncoder.matches(oldPassword, userCredentials.passwordHash())) {
            throw new IncorrectPasswordException("Provided password does not match current one");
        }

        if (passwordEncoder.matches(newPassword, userCredentials.passwordHash())) {
            throw new BadRequestException("New password must be different from current one");
        }

        updatePasswordHashUseCase.updatePasswordHash(userId, passwordEncoder.encode(newPassword));
        log.info("Password hash updated for user id: {}", userId);

        refreshTokenRepository.revokeByUserId(userId);
        log.info("All refresh tokens revoked for user id: {}", userId);

        eventPublisher.publishEvent(new PasswordChanged(userCredentials.username(), userCredentials.email(), now));
        log.info("PasswordChanged event published for user id: {}", userId);
    }
}
