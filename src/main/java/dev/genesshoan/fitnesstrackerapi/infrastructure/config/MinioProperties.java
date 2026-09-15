package dev.genesshoan.fitnesstrackerapi.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.minio")
public record MinioProperties(
        String endpoint, String bucket, String accessKey, String secretKey, Duration presignedUrlExpiration) {}
