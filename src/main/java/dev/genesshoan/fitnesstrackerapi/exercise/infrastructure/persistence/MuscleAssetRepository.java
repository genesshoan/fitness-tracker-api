package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.MuscleAsset;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.persistence.projection.MuscleBaseAssetsProjection;

/**
 * Persistence operations for the muscle catalog assets.
 */
public interface MuscleAssetRepository extends JpaRepository<MuscleAsset, UUID> {

    /**
     * Retrieves the base assets used for the muscle visualization.
     *
     * @return projection containing the object storage keys of the front and
     *         back base assets
     */
    @Query(value = """
            SELECT
                MAX(CASE WHEN a.view = 'FRONT' THEN a.object_key END) AS frontBaseObjectKey,
                MAX(CASE WHEN a.view = 'BACK' THEN a.object_key END) AS backBaseObjectKey
            FROM muscle_assets a
            WHERE a.object_key ILIKE '%base%'
        """, nativeQuery = true)
    MuscleBaseAssetsProjection findBaseAssets();
}
