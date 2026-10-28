package dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProxyClientIpResolver implements ClientIpResolver {

    private final ClientIpProperties properties;

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

    private String lastForwardedFor(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }

        String[] parts = header.split(",");
        return parts[parts.length - 1].trim();
    }
}
