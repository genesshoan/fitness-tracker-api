package dev.genesshoan.fitnesstrackerapi.auth.application.service;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.auth.application.command.RegisterCommand;
import dev.genesshoan.fitnesstrackerapi.auth.application.event.VerificationEmailRequested;
import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationPurpose;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.InvalidCodeException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.user.UserRepository;
import dev.genesshoan.fitnesstrackerapi.user.domain.Role;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final VerificationTokenService verificationTokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
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

        var user = User.builder()
                .username(command.username())
                .passwordHash(passwordEncoder.encode(command.password()))
                .email(command.email())
                .timezone(command.timezone())
                .role(Role.USER)
                .build();

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException cve) {
                String constraint = cve.getConstraintName();
                if (constraint != null && constraint.contains("email")) {
                    throw new ResourceAlreadyExistsException("Email already exists", e);
                }
                if (constraint != null && constraint.contains("username")) {
                    throw new ResourceAlreadyExistsException("Username already exists", e);
                }
            }
            throw e;
        }

        log.info("User registered successfully. userId={}, email={}", user.getId(), user.getEmail());

        issueAndPublishVerification(user);
    }

    /**
     * Verifies an email verification token and marks the user as verified.
     *
     * <p>
     * <b>Returns:</b> <b>void</b> - the token is consumed and the user is marked as verified.
     * <b>Throws:</b> <b>InvalidCodeException</b> if the token is invalid, expired, or already used.
     * <b>Throws:</b> <b>ResourceAlreadyExistsException</b> if the user does not exist.
     */
    @Transactional
    public void verifyEmail(String token) {
        UUID userId = verificationTokenService
                .consume(VerificationPurpose.EMAIL_VERIFICATION, token)
                .orElseThrow(
                        () -> new InvalidCodeException("Email verification token is invalid, expired or already use"));

        if (userRepository.markEmailVerified(userId) == 0) {
            throw new InvalidCodeException(
                    "Email verification token was valid but the user no longer exists. userId=" + userId);
        }
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
        userRepository
                .findByEmail(email)
                .filter(user -> !user.isEmailVerified())
                .ifPresentOrElse(
                        user -> issueAndPublishVerification(user),
                        () -> log.info("Resend verification ignored: unknown or already verified account"));
    }

    /**
     * Issues a new verification token and publishes a verification email event.
     *
     * @param user the user for whom to issue a verification token
     */
    private void issueAndPublishVerification(User user) {
        var issued = verificationTokenService.issue(user.getId(), VerificationPurpose.EMAIL_VERIFICATION);
        eventPublisher.publishEvent(new VerificationEmailRequested(
                user.getUsername(), user.getEmail(), issued.token(), issued.expiresAt()));
    }
}
