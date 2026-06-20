package dev.genesshoan.fitnesstrackerapi.exercise.muscle.mapper;

import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.Muscle;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.dto.MuscleResponseDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface MuscleMapper {

    MuscleResponseDTO toResponseDTO(Muscle muscle);
}
