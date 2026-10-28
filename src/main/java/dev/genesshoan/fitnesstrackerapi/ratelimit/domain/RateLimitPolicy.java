package dev.genesshoan.fitnesstrackerapi.ratelimit.domain;

/**
 * Logical policies that can be rate limited.
 *
 * <p>Each value maps to an entry under {@code app.rate-limit.policies} in the
 * application configuration, so the limits themselves are not hard-coded.
 * Endpoints opt into a policy with
 * {@link dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.RateLimited @RateLimited}.
 */
public enum RateLimitPolicy {

    /** Authentication attempts with email and password. */
    LOGIN,

    /** Account creation requests. */
    REGISTRATION,

    /** Requests that resend an email verification link. */
    EMAIL_VERIFICATION,

    /** Requests that trigger a password reset email. */
    PASSWORD_RESET
}
