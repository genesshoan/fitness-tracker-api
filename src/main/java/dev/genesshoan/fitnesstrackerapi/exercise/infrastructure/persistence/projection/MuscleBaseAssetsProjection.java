package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.persistence.projection;

/**
 * Projection containing the object storage keys of the base assets used for
 * the muscle visualization.
 */
public interface MuscleBaseAssetsProjection {

    String getFrontBaseObjectKey();

    String getBackBaseObjectKey();
}
