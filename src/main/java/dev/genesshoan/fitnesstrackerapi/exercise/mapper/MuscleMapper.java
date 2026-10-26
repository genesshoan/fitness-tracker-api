package dev.genesshoan.fitnesstrackerapi.exercise.mapper;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.Muscle;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.MuscleAsset;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web.dto.MuscleAssetResponse;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web.dto.MuscleResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface MuscleMapper {

    MuscleResponse toResponseDTO(Muscle muscle);

    MuscleAssetResponse toAssetResponseDTO(MuscleAsset asset);
}
