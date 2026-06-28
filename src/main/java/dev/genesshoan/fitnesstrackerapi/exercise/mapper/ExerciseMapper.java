package dev.genesshoan.fitnesstrackerapi.exercise.mapper;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseDetailDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseListItemDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(
    componentModel = "spring",
    builder = @Builder(disableBuilder = true),
    uses = { ExerciseMuscleMapper.class }
)
public interface ExerciseMapper {
    ExerciseListItemDTO toItemDTO(Exercise exercise);

    ExerciseDetailDTO toDetailDTO(Exercise exercise);
}
