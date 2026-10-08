package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for public catalog asset URLs.
 *
 * @param baseUrl public base URL used to resolve catalog assets
 * @param exercise paths for exercise assets
 * @param muscle paths for muscle assets
 */
@ConfigurationProperties(prefix = "app.file.get.catalog")
public record CatalogProperties(String baseUrl, Exercise exercise, Muscle muscle) {

    /**
     * Configuration for exercise asset paths.
     *
     * @param gifs path for exercise GIF assets
     * @param thumbnails path for exercise thumbnail assets
     */
    public record Exercise(String gifs, String thumbnails) {}

    /**
     * Configuration for muscle asset paths.
     *
     * @param images path for muscle image assets
     */
    public record Muscle(String images) {}
}
