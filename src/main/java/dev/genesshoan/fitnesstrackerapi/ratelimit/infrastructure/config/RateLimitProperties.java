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

@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @NotEmpty @Valid Map<RateLimitPolicy, PolicyConfig> policies,
        @DefaultValue("64") @Min(1) @Max(128) int ipv6Prefix) {

    public record PolicyConfig(
            @Valid List<LimitConfig> ip, @Valid List<LimitConfig> account) {}

    public record LimitConfig(
            @Min(1) int capacity,
            @NotNull Duration period,
            @DefaultValue("GREEDY") RefillType refill) {}

    public enum RefillType {
        GREEDY,
        INTERVALLY
    }
}
