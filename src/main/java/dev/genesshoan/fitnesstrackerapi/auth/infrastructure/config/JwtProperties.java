package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.config;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Access-token (JWT) settings, bound to {@code app.auth.jwt}.
 */
@Validated
@ConfigurationProperties(prefix = "app.auth.jwt")
public record JwtProperties(
        @NotBlank String secret, @NotNull Duration accessTokenExpiration) {

    @Override
    public String toString() {
        return "JwtProperties[secret=***, accessTokenExpiration=" + accessTokenExpiration + "]";
    }
}
