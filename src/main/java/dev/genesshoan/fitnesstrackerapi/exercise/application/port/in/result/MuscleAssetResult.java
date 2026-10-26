package dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result;

import java.util.UUID;

/**
 * An image that illustrates a muscle.
 *
 * @param id        the asset id
 * @param objectKey the key of the asset in the object storage
 * @param variant   the asset variant
 * @param view      the view the asset shows
 */
public record MuscleAssetResult(UUID id, String objectKey, String variant, String view) {}
