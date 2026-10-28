package dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the client IP address used to key IP based rate limits.
 *
 * <p>Implementations decide which proxy header to trust and which fallback to
 * use. The returned value is normalized later by {@link IpNormalizer}, so it may
 * be an IPv4, IPv6 or malformed string.
 */
public interface ClientIpResolver {

    /**
     * Extracts the client address from the current request.
     *
     * @param request request being processed
     * @return the client IP, or the remote address when the configured header is
     *     absent
     */
    String resolve(HttpServletRequest request);
}
