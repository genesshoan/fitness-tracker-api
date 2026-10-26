package dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result;

/**
 * The base assets used for the muscle visualization.
 *
 * @param frontBaseAssetUrl the public URL of the front view base asset
 * @param backBaseAssetUrl  the public URL of the back view base asset
 */
public record MuscleBaseAssetsResult(String frontBaseAssetUrl, String backBaseAssetUrl) {}
