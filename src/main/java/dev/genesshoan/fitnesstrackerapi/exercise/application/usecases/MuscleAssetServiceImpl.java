package dev.genesshoan.fitnesstrackerapi.exercise.application.usecases;

import org.springframework.stereotype.Service;

import dev.genesshoan.fitnesstrackerapi.exercise.application.ports.inbound.MuscleAssetUrlPort;
import dev.genesshoan.fitnesstrackerapi.exercise.application.ports.outbound.AssetUrlProviderPort;
import dev.genesshoan.fitnesstrackerapi.exercise.application.ports.outbound.MuscleAssetRepositoryPort;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.MuscleBaseAssetsDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.persistence.projection.MuscleBaseAssetsProjection;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MuscleAssetServiceImpl implements MuscleAssetUrlPort {

    private final AssetUrlProviderPort assetUrlProviderPort;
    private final MuscleAssetRepositoryPort muscleAssetRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    public MuscleBaseAssetsDTO findBaseAssets() {
        MuscleBaseAssetsProjection objectkeys = muscleAssetRepositoryPort.findBaseAssets();

        return new MuscleBaseAssetsDTO(
                assetUrlProviderPort.muscleUrl(objectkeys.getFrontBaseObjectKey()),
                assetUrlProviderPort.muscleUrl(objectkeys.getBackBaseObjectKey()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String muscleUrl(String objectKey) {
        return assetUrlProviderPort.muscleUrl(objectKey);
    }
}
