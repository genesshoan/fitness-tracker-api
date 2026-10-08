package dev.genesshoan.fitnesstrackerapi.exercise.application.ports.inbound;

import dev.genesshoan.fitnesstrackerapi.exercise.dto.MuscleBaseAssetsDTO;

/**
 * Outbound port for resolving public URLs of muscle visualization assets.
 *
 * <p>This port abstracts the mechanism used to expose catalog muscle assets
 * as publicly accessible URLs, allowing the application layer to remain
 * independent of the underlying asset storage or URL generation strategy.</p>
 */
public interface MuscleAssetUrlPort {

    /**
     * Retrieves the base assets used for the muscle visualization.
     *
     * @return projection containing the front and back base assets
     */
    MuscleBaseAssetsDTO findBaseAssets();

    /**
     * Resolves the public URL of a muscle image.
     *
     * @param objectKey object storage key of the image
     * @return public URL of the muscle asset, or {@code null} if the object key is {@code null}
     */
    String muscleUrl(String objectKey);
}
