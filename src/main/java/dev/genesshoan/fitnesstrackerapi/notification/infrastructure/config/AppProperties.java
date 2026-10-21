package dev.genesshoan.fitnesstrackerapi.notification.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String name, @Valid @NotNull Mail mail) {
    public record Mail(String logoUrl, @NotBlank String frontendUrl) {}
}
