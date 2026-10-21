package dev.genesshoan.fitnesstrackerapi.auth.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.auth.application.event.PasswordChanged;
import dev.genesshoan.fitnesstrackerapi.auth.application.event.PasswordResetRequested;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.IncorrectPasswordException;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.InvalidCodeException;
import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationPurpose;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence.RefreshTokenRepository;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.GetUserCredentialsUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.UpdatePasswordHashUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialsService {

    private final GetUserCredentialsUseCase getUserCredentialsUseCase;
    private final UpdatePasswordHashUseCase updatePasswordHashUseCase;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenService verificationTokenService;
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

    /**
     * Issues a password reset token and requests the reset email.
     *
     * <p>The reset token is issued with the {@link VerificationPurpose#PASSWORD_RESET}
     * purpose, so it cannot be used for any other security operation, and it replaces
     * any reset token previously issued for the same account.
     *
     * <p>When no account matches the given email the request is silently ignored, so
     * the response never reveals whether an email is registered.
     *
     * @param email the email address of the account requesting the reset
     */
    @Transactional
    public void requestPasswordReset(String email) {
        log.info("Password reset requested for email={}", email);

        getUserCredentialsUseCase
                .findByEmail(email)
                .ifPresentOrElse(
                        credentials -> issueAndPublishPasswordReset(
                                credentials.id(), credentials.username(), credentials.email()),
                        () -> log.info("Password reset ignored: unknown account"));
    }

    /**
     * Resets the user's password using a password reset token.
     *
     * <p>The token is consumed, the password hash is replaced, and every refresh
     * token of the account is revoked, so all sessions must log in again.
     *
     * @param token the raw password reset token
     * @param newPassword the new password
     * @throws InvalidCodeException if the token is invalid, expired, or already used
     * @throws BadRequestException  if the new password matches the current one
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Resetting password");

        UUID userId = verificationTokenService
                .consume(VerificationPurpose.PASSWORD_RESET, token)
                .orElseThrow(
                        () -> new InvalidCodeException("Password reset token is invalid, expired or already used"));

        var userCredentials = getUserCredentialsUseCase
                .findById(userId)
                .orElseThrow(() -> new InvalidCodeException(
                        "Password reset token was valid but the user no longer exists. userId=" + userId));

        if (passwordEncoder.matches(newPassword, userCredentials.passwordHash())) {
            throw new BadRequestException("New password must be different from current one");
        }

        updatePasswordHashUseCase.updatePasswordHash(userId, passwordEncoder.encode(newPassword));
        log.info("Password hash updated for user id: {}", userId);

        refreshTokenRepository.revokeByUserId(userId);
        log.info("All refresh tokens revoked for user id: {}", userId);

        eventPublisher.publishEvent(
                new PasswordChanged(userCredentials.username(), userCredentials.email(), clock.instant()));
        log.info("PasswordChanged event published for user id: {}", userId);
    }

    /**
     * Issues a new password reset token and publishes the password reset event.
     *
     * @param userId the user id
     * @param username the user username
     * @param email the user email
     */
    private void issueAndPublishPasswordReset(UUID userId, String username, String email) {
        var issued = verificationTokenService.issue(userId, VerificationPurpose.PASSWORD_RESET);
        eventPublisher.publishEvent(new PasswordResetRequested(username, email, issued.token(), issued.expiresAt()));
        log.info("PasswordResetRequested event published for user id: {}", userId);
    }
}
