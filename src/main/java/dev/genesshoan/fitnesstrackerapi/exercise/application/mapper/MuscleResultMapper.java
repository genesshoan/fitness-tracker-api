package dev.genesshoan.fitnesstrackerapi.exercise.application.mapper;

import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.MuscleAssetResult;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.MuscleResult;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Muscle;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.MuscleAsset;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

/**
 * Maps the muscle catalog to the values the application layer exposes.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface MuscleResultMapper {

    /**
     * Maps a muscle to its application result.
     *
     * @param muscle the muscle to map
     * @return the muscle result, including the assets that illustrate it
     */
    MuscleResult toResult(Muscle muscle);

    /**
     * Maps a muscle asset to its application result.
     *
     * @param asset the asset to map
     * @return the asset result
     */
    MuscleAssetResult toAssetResult(MuscleAsset asset);
}
