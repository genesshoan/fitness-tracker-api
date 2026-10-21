package dev.genesshoan.fitnesstrackerapi.auth.application.event;

import java.time.Instant;

/**
 * Application event requesting a password reset email.
 *
 * <p>Published by the authentication module once a password reset token has been
 * issued. The notification module listens to this event to send the reset email,
 * keeping the publisher unaware of the email templates and delivery details.
 *
 * @param username  the username of the account requesting the reset
 * @param email     the email address the reset email is sent to
 * @param token     the raw password reset token
 * @param expiresAt the instant the token expires
 */
public record PasswordResetRequested(String username, String email, String token, Instant expiresAt) {}
