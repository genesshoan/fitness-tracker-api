package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure;

import org.springframework.stereotype.Component;

import dev.genesshoan.fitnesstrackerapi.exercise.application.ports.outbound.AssetUrlProviderPort;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.config.CatalogProperties;
import lombok.RequiredArgsConstructor;

/**
 * Infrastructure adapter that resolves catalog asset keys into public URLs.
 */
@Component
@RequiredArgsConstructor
public class CatalogAssetUrlProvider implements AssetUrlProviderPort {

    private final CatalogProperties properties;

    /**
     * {@inheritDoc}
     */
    @Override
    public String exerciseGifUrl(String objectKey) {
        return build(properties.exercise().gifs(), objectKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String exerciseThumbnailUrl(String objectKey) {
        return build(properties.exercise().thumbnails(), objectKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
