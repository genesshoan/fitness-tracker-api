package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.genesshoan.fitnesstrackerapi.exercise.application.ports.outbound.MuscleAssetRepositoryPort;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.MuscleAsset;

public interface MuscleAssetRepository extends JpaRepository<MuscleAsset, UUID>, MuscleAssetRepositoryPort {

    /**
     * {@inheritDoc}
     */
    @Override
    Optional<MuscleAsset> findByObjectKey(String objectKey);
}
