package dev.genesshoan.fitnesstrackerapi.exercise.application.port.in;

import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.MuscleBaseAssetsResult;

/**
 * Use case for resolving the public URLs of the muscle visualization assets.
 *
 * <p>Implemented by the exercise module, which owns the catalog assets, and
 * consumed by the modules that render the muscle intensity visualization.
 */
public interface MuscleAssetUseCase {

    /**
     * Retrieves the base assets used for the muscle visualization.
     *
     * @return the public URLs of the front and back base assets
     */
    MuscleBaseAssetsResult findBaseAssets();

    /**
     * Resolves the public URL of a muscle image.
     *
     * @param objectKey object storage key of the image
     * @return public URL of the muscle asset, or {@code null} if the object key is {@code null}
     */
    String resolveMuscleUrl(String objectKey);
}
