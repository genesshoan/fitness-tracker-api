package dev.genesshoan.fitnesstrackerapi.infrastructure.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record EmailProperties(String from, String fromName) {}
