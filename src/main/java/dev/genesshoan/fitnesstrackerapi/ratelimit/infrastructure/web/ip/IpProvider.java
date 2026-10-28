package dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip;

/**
 * Supported sources for the client IP address.
 *
 * <p>The selected value must match the proxy in front of the application,
 * because only a header controlled by that proxy can be trusted.
 */
public enum IpProvider {

    /** Reads {@code CF-Connecting-IP}, set by Cloudflare. */
    CLOUDFLARE,

    /** Reads the last entry of {@code X-Forwarded-For}. */
    GENERIC_PROXY,

    /** Ignores proxy headers and uses the connection's remote address. */
    DIRECT
}
