package dev.genesshoan.fitnesstrackerapi.exercise.application.ports.outbound;

import java.util.Optional;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.MuscleAsset;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.persistence.projection.MuscleBaseAssetsProjection;

/**
 * Port for accessing muscle asset persistence.
 *
 * <p>Provides access to muscle assets stored using their object storage key.
 */
public interface MuscleAssetRepositoryPort {

    /**
     * Retrieves the base assets used for the muscle visualization.
     *
     * @return projection containing the front and back base assets
     */
    MuscleBaseAssetsProjection findBaseAssets();

    /**
     * Finds a muscle asset by its object storage key.
     *
     * @param objectKey the key identifying the asset in object storage
     * @return the matching muscle asset, or {@link Optional#empty()} if no
     *         asset with the specified object key exists
     */
    Optional<MuscleAsset> findByObjectKey(String objectKey);
}
