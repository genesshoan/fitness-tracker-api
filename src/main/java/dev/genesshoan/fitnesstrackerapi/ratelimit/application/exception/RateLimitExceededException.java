package dev.genesshoan.fitnesstrackerapi.ratelimit.application.exception;

/**
 * Signals that a request exceeded one of the configured rate limits.
 *
 * <p>Carries the number of seconds the client should wait before retrying. The
 * web layer exposes that value through the {@code Retry-After} response header.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    /**
     * Creates the exception.
     *
     * @param retryAfterSeconds seconds to wait before the next attempt is likely
     *     to succeed; always at least one
     */
    public RateLimitExceededException(long retryAfterSeconds) {
        super("Too many requests");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Returns how long the client should wait before retrying.
     *
     * @return the wait time in seconds
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
