package dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record ClientIpProperties(@NotNull IpProvider ipProvider) {}
