package dev.genesshoan.fitnesstrackerapi.exercise.mapper;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.Muscle;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.MuscleAssetResponseDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.MuscleResponseDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface MuscleMapper {

    MuscleResponseDTO toResponseDTO(Muscle muscle);

    MuscleAssetResponseDTO toAssetResponseDTO(
            dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.MuscleAsset asset);
}
