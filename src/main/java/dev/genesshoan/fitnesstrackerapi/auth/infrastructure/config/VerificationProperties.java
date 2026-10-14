package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.config;

import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import org.hibernate.validator.constraints.Length;

/**
 * Properties configuration for verification code system.
 *
 * <p>
 * Binds to application configuration properties under the {@code app.verification}
 * prefix. Defines constraints and default values for verification code generation
 * and validation, including:
 * <ul>
 * <li>Secret key length requirements for HMAC signing</li>
 * <li>Default expiration durations for codes and tokens</li>
 * <li>Maximum allowed verification attempts before account lockout</li>
 * <li>Allowed code length (number of characters)</li>
 * </ul>
 *
 * <p>
 * All properties are validated on initialization via Jakarta Bean Validation.
 */
@Validated
@ConfigurationProperties(prefix = "app.verification")
public record VerificationProperties(
        @Length(min = 32) String secret,
        @NotNull Duration codeExpiration,
        @NotNull Duration tokenExpiration,
        @Min(1) int maxAttempts,
        @Min(6) @Max(9) String codeLength) {}
