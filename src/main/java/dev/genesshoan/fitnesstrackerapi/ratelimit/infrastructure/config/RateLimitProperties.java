package dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import dev.genesshoan.fitnesstrackerapi.ratelimit.domain.RateLimitPolicy;

/**
 * Rate limit settings, bound to {@code app.rate-limit}.
 *
 * <p>The configuration is validated at startup so a malformed limit cannot
 * silently disable protection: every policy needs at least one entry, capacities
 * must be positive, periods are required and the IPv6 prefix must be valid.
 *
 * @param policies limits configured for each policy, keyed by policy name
 * @param ipv6Prefix prefix used to group IPv6 clients; defaults to {@code 64}
 */
@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @NotEmpty @Valid Map<RateLimitPolicy, PolicyConfig> policies,
        @DefaultValue("64") @Min(1) @Max(128) int ipv6Prefix) {

    /**
     * Limits that apply to a policy, split by dimension.
     *
     * @param ip bandwidths applied to the client IP
     * @param account bandwidths applied to the account identifier
     */
    public record PolicyConfig(
            @Valid List<LimitConfig> ip, @Valid List<LimitConfig> account) {}

    /**
     * A single bandwidth: how many requests are allowed per period and how the
     * quota is restored.
     *
     * @param capacity maximum number of requests in the period; must be positive
     * @param period length of the window
     * @param refill refill strategy, {@link RefillType#GREEDY} by default
     */
    public record LimitConfig(
            @Min(1) int capacity,
            @NotNull Duration period,
            @DefaultValue("GREEDY") RefillType refill) {}

    /** How a bucket recovers capacity after requests are consumed. */
    public enum RefillType {

        /** Restores tokens continuously over the period. */
        GREEDY,

        /** Restores the full capacity at the end of each period. */
        INTERVALLY
    }
}
