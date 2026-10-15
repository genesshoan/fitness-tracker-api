package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.config;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Refresh-token settings, bound to {@code app.auth.refresh-token}.
 */
@Validated
@ConfigurationProperties(prefix = "app.auth.refresh-token")
public record RefreshTokenProperties(@NotNull Duration expiration, boolean cookieSecure) {}
