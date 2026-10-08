package dev.genesshoan.fitnesstrackerapi.exercise.application.ports.outbound;

/**
 * Outbound port for resolving public URLs for catalog assets.
 */
public interface AssetUrlProviderPort {

    /**
     * Resolves the public URL of an exercise GIF.
     *
     * @param objectKey object storage key of the GIF
     * @return public URL of the exercise GIF, or {@code null} if the object key is {@code null}
     */
    String exerciseGifUrl(String objectKey);

    /**
     * Resolves the public URL of an exercise thumbnail.
     *
     * @param objectKey object storage key of the thumbnail
     * @return public URL of the exercise thumbnail, or {@code null} if the object key is {@code null}
     */
    String exerciseThumbnailUrl(String objectKey);

    /**
     * Resolves the public URL of a muscle image.
     *
     * @param objectKey object storage key of the image
     * @return public URL of the muscle image, or {@code null} if the object key is {@code null}
     */
    String muscleUrl(String objectKey);
}
