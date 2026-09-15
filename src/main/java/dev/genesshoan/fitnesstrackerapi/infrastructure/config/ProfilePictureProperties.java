package dev.genesshoan.fitnesstrackerapi.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "file.upload.profile-picture")
public record ProfilePictureProperties(long maxSize, List<String> allowedTypes) {}
