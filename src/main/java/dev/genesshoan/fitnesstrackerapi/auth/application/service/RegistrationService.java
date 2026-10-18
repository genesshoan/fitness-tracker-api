package dev.genesshoan.fitnesstrackerapi.auth.application.service;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.auth.application.command.RegisterCommand;
import dev.genesshoan.fitnesstrackerapi.auth.application.event.VerificationEmailRequested;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.InvalidCodeException;
import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationPurpose;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.RegisterUserUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.VerifyUserEmailUseCase;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.command.RegisterUserCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final VerificationTokenService verificationTokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final RegisterUserUseCase registerUserUseCase;
    private final VerifyUserEmailUseCase verifyUserEmailUseCase;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user account.
     *
     * <p>
     * Validates that the username and email are not already taken,
     * encodes the password using BCrypt, persists the user, and
     * issues a verification email to the provided address.
     *
     * @param command the registration request containing username, email and password
     * @throws ResourceAlreadyExistsException if the username or email is already
     *                                        registered
     */
    @Transactional
    public void register(RegisterCommand command) {
        log.debug("Registration attempt. email={}, username={}", command.email(), command.username());

        UUID userId = registerUserUseCase.register(new RegisterUserCommand(
                command.email(), command.username(), command.timezone(), passwordEncoder.encode(command.password())));

        log.info("User registered successfully. userId={}, email={}", userId, command.email());

        issueAndPublishVerification(userId, command.username(), command.email());
    }

    /**
     * Verifies an email verification token and marks the user as verified.
     *
     * <p>
     * The token is consumed and the user is marked as verified.
     *
     * @param token the raw verification token string
     * @throws InvalidCodeException if the token is invalid, expired, or already used.
     */
    @Transactional
    public void verifyEmail(String token) {
        log.info("Verifying email token");
        UUID userId = verificationTokenService
                .consume(VerificationPurpose.EMAIL_VERIFICATION, token)
                .orElseThrow(
                        () -> new InvalidCodeException("Email verification token is invalid, expired or already used"));

        if (!verifyUserEmailUseCase.markEmailVerified(userId)) {
            throw new InvalidCodeException(
                    "Email verification token was valid but the user no longer exists. userId=" + userId);
        }

        log.info("Email verified successfully. userId={}", userId);
    }

    /**
     * Sends a new email verification token to an existing user.
     *
     * <p>
     * Looks up the user by email and issues a fresh verification code if the
     * account exists and is not yet verified. If the user does not exist or
     * the account is already verified, the request is silently ignored.
     *
     * @param email the email address to send the verification code to
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        verifyUserEmailUseCase
                .findUnverifiedByEmail(email)
                .ifPresentOrElse(
                        user -> issueAndPublishVerification(user.id(), user.username(), email),
                        () -> log.info("Resend verification ignored: unknown or already verified account"));
    }

    /**
     * Issues a new verification token and publishes a verification email event.
     *
     * @param userId the user id
     * @param username the user username
     * @param email the user email
     */
    private void issueAndPublishVerification(UUID userId, String username, String email) {
        var issued = verificationTokenService.issue(userId, VerificationPurpose.EMAIL_VERIFICATION);
        eventPublisher.publishEvent(
                new VerificationEmailRequested(username, email, issued.token(), issued.expiresAt()));
    }
}
