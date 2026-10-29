package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for public catalog asset URLs.
 *
 * <p>The catalog base URL is the root of the public bucket. Every asset key
 * already carries its own path inside the bucket, so the URL of an asset is
 * the base URL followed by the key.
 *
 * @param baseUrl public base URL used to resolve catalog assets
 */
@ConfigurationProperties(prefix = "app.file.get.catalog")
public record CatalogProperties(String baseUrl) {}
