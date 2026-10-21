package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.config;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Expiration settings for single-use verification tokens, bound to the
 * {@code app.verification} prefix and validated at startup.
 */
@Validated
@ConfigurationProperties(prefix = "app.verification")
public record VerificationProperties(
        @NotNull Duration emailVerificationExpiration,
        @NotNull Duration passwordResetExpiration) {}
