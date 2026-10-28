package dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Resolves the client IP from the header configured through
 * {@link ClientIpProperties}.
 *
 * <p>When the configured header is missing or blank it falls back to
 * {@link HttpServletRequest#getRemoteAddr()}, so the application still has a
 * usable identifier even behind an unexpected proxy.
 */
@Component
@RequiredArgsConstructor
public class ProxyClientIpResolver implements ClientIpResolver {

    private final ClientIpProperties properties;

    /**
     * {@inheritDoc}
     *
     * <p>The header is chosen from {@link ClientIpProperties#ipProvider()}; its
     * value is trimmed and falls back to the remote address when absent.
     */
    @Override
    public String resolve(HttpServletRequest request) {
        String ip =
                switch (properties.ipProvider()) {
                    case CLOUDFLARE -> request.getHeader("CF-Connecting-IP");
                    case GENERIC_PROXY -> lastForwardedFor(request.getHeader("X-Forwarded-For"));
                    case DIRECT -> null;
                };

        return (ip == null || ip.isBlank()) ? request.getRemoteAddr() : ip.trim();
    }

    /**
     * Returns the last address of an {@code X-Forwarded-For} header.
     *
     * <p>The last entry is the one appended by the closest proxy, which is the
     * value the application can trust the most when the chain is configured
     * correctly.
     *
     * @param header raw header value, possibly {@code null}
     * @return the trimmed last address, or {@code null} when the header is absent
     */
    private String lastForwardedFor(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }

        String[] parts = header.split(",");
        return parts[parts.length - 1].trim();
    }
}
