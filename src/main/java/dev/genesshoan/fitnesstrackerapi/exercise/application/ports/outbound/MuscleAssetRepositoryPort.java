package dev.genesshoan.fitnesstrackerapi.exercise.application.ports.outbound;

import java.util.Optional;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.MuscleAsset;

/**
 * Port for accessing muscle asset persistence.
 *
 * <p>Provides access to muscle assets stored using their object storage key.
 */
public interface MuscleAssetRepositoryPort {

    /**
     * Finds a muscle asset by its object storage key.
     *
     * @param objectKey the key identifying the asset in object storage
     * @return the matching muscle asset, or {@link Optional#empty()} if no
     *         asset with the specified object key exists
     */
    Optional<MuscleAsset> findByObjectKey(String objectKey);
}
