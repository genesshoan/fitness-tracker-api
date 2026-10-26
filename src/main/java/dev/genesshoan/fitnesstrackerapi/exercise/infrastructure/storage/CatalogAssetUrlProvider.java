package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.storage;

import org.springframework.stereotype.Component;

import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.config.CatalogProperties;
import lombok.RequiredArgsConstructor;

/**
 * Infrastructure adapter that resolves catalog asset keys into public URLs.
 */
@Component
@RequiredArgsConstructor
public class CatalogAssetUrlProvider {

    private final CatalogProperties properties;

    /**
     * Resolves the public URL of an exercise GIF.
     *
     * @param objectKey object storage key of the GIF
     * @return public URL of the exercise GIF, or {@code null} if the object key is {@code null}
     */
    public String exerciseGifUrl(String objectKey) {
        return build(properties.exercise().gifs(), objectKey);
    }

    /**
     * Resolves the public URL of an exercise thumbnail.
     *
     * @param objectKey object storage key of the thumbnail
     * @return public URL of the exercise thumbnail, or {@code null} if the object key is {@code null}
     */
    public String exerciseThumbnailUrl(String objectKey) {
        return build(properties.exercise().thumbnails(), objectKey);
    }

    /**
     * Resolves the public URL of a muscle image.
     *
     * @param objectKey object storage key of the image
     * @return public URL of the muscle image, or {@code null} if the object key is {@code null}
     */
    public String muscleUrl(String objectKey) {
        return build(properties.muscle().images(), objectKey);
    }

    private String build(String path, String objectKey) {
        if (objectKey == null) {
            return null;
        }

        return properties.baseUrl() + "/" + path + "/" + objectKey;
    }
}
