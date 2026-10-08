package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.genesshoan.fitnesstrackerapi.exercise.application.ports.outbound.MuscleAssetRepositoryPort;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.MuscleAsset;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.persistence.projection.MuscleBaseAssetsProjection;

public interface MuscleAssetRepository extends JpaRepository<MuscleAsset, UUID>, MuscleAssetRepositoryPort {

    /**
     * {@inheritDoc}
     */
    @Override
    @Query(value = """
            SELECT
                MAX(CASE WHEN a.view = 'FRONT' THEN a.object_key END) AS frontBaseObjectKey,
                MAX(CASE WHEN a.view = 'BACK' THEN a.object_key END) AS backBaseObjectKey
            FROM muscle_assets a
            WHERE a.object_key ILIKE '%base%'
        """, nativeQuery = true)
    MuscleBaseAssetsProjection findBaseAssets();

    /**
     * {@inheritDoc}
     */
    @Override
    Optional<MuscleAsset> findByObjectKey(String objectKey);
}
