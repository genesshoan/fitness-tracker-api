package dev.genesshoan.fitnesstrackerapi.security;

import java.time.Duration;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cross-origin resource sharing settings, bound to {@code app.cors}.
 *
 * <p>The allowed origins are treated as origin patterns, so both exact origins
 * ({@code https://app.example.com}) and wildcards ({@code https://*.example.com})
 * are accepted. Patterns are used instead of plain origins because the refresh
 * token is delivered through a cookie, which forces credentials on the CORS
 * requests and forbids the {@code *} special value in an allowed origins list.
 *
 * <p>Every value can be overridden with an environment variable, see
 * {@code application.yaml}.
 */
@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @NotEmpty List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        List<String> exposedHeaders,
        boolean allowCredentials,
        Duration maxAge) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins;
        allowedMethods = allowedMethods == null ? List.of() : allowedMethods;
        allowedHeaders = allowedHeaders == null ? List.of() : allowedHeaders;
        exposedHeaders = exposedHeaders == null ? List.of() : exposedHeaders;
    }
}
