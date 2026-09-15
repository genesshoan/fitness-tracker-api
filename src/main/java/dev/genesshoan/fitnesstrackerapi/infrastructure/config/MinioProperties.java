package dev.genesshoan.fitnesstrackerapi.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.minio")
public record MinioProperties(String endpoint, String bucket, String accessKey, String secretKey) {}
