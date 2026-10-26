package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web;

import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.MuscleAssetResult;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.MuscleResult;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web.dto.MuscleAssetResponse;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web.dto.MuscleResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

/**
 * Maps the muscle results to their API representations.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface MuscleMapper {

    /**
     * Maps a muscle result to its API representation.
     *
     * @param result the result to map
     * @return the muscle response
     */
    MuscleResponse toResponseDTO(MuscleResult result);

    /**
     * Maps a muscle asset result to its API representation.
     *
     * @param result the result to map
     * @return the muscle asset response
     */
    MuscleAssetResponse toAssetResponseDTO(MuscleAssetResult result);
}
