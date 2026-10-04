package dev.genesshoan.fitnesstrackerapi.infrastructure.storage.s3.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for S3-compatible object storage.
 *
 * <p>Bound from {@code app.storage} prefix in application configuration.
 * Supports S3-compatible storage services like AWS S3, MinIO, and Backblaze B2.
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        boolean pathStyle,
        Duration presignedUrlExpiration) {}
