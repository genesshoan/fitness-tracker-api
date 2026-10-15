package dev.genesshoan.fitnesstrackerapi.auth.application.result;

import java.time.Instant;

/**
 * Result record representing an issued verification token with its expiration.
 *
 * <p>Contains the raw token string and its expiration timestamp, returned by
 * {@link VerificationTokenService#issue(UUID, VerificationPurpose)}. The token
 * should be published via {@link ApplicationEventPublisher} as a
 * {@link VerificationEmailRequested} event.
 *
 * @param token   the raw verification token string
 * @param expiresAt the expiration timestamp for the token
 * @see VerificationTokenService#issue(UUID, VerificationPurpose)
 */
public record IssuedToken(String token, Instant expiresAt) {}
