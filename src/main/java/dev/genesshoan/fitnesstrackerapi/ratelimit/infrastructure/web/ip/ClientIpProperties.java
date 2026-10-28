package dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Client IP resolution settings, bound to {@code app.security}.
 *
 * <p>Selects which proxy header is trusted to carry the original client
 * address. The choice must match the deployment: trusting a header that clients
 * can forge would let them bypass IP based rate limits.
 *
 * @param ipProvider source of the client IP
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record ClientIpProperties(@NotNull IpProvider ipProvider) {}
