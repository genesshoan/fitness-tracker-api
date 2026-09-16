package dev.genesshoan.fitnesstrackerapi.infrastructure.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail")
public record EmailProperties(String from, String fromName) {}
