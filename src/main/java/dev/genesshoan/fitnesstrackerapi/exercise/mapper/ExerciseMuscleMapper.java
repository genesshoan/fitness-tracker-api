package dev.genesshoan.fitnesstrackerapi.exercise.mapper;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.ExerciseMuscle;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseMuscleDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.mapper.MuscleMapper;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(
    componentModel = "spring",
    builder = @Builder(disableBuilder = true),
    uses = { MuscleMapper.class }
)
public interface ExerciseMuscleMapper {
    ExerciseMuscleDTO toExerciseMuscleDto(ExerciseMuscle exerciseMuscle);
}
