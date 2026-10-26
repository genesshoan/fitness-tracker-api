package dev.genesshoan.fitnesstrackerapi.exercise.application.service;

import org.springframework.stereotype.Service;

import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.MuscleAssetUseCase;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.MuscleBaseAssetsResult;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.persistence.MuscleAssetRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.storage.CatalogAssetUrlProvider;
import lombok.RequiredArgsConstructor;

/**
 * Service that resolves the public URLs of the muscle catalog assets.
 */
@Service
@RequiredArgsConstructor
public class MuscleAssetService implements MuscleAssetUseCase {

    private final MuscleAssetRepository muscleAssetRepository;
    private final CatalogAssetUrlProvider assetUrlProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    public MuscleBaseAssetsResult findBaseAssets() {
        var objectKeys = muscleAssetRepository.findBaseAssets();

        return new MuscleBaseAssetsResult(
                assetUrlProvider.muscleUrl(objectKeys.getFrontBaseObjectKey()),
                assetUrlProvider.muscleUrl(objectKeys.getBackBaseObjectKey()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolveMuscleUrl(String objectKey) {
        return assetUrlProvider.muscleUrl(objectKey);
    }
}
