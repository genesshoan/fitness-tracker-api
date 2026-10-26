package dev.genesshoan.fitnesstrackerapi.exercise.mapper;

import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web.dto.ExerciseMuscle;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        uses = {MuscleMapper.class})
public interface ExerciseMuscleMapper {
    ExerciseMuscle toExerciseMuscleDto(dev.genesshoan.fitnesstrackerapi.exercise.domain.ExerciseMuscle exerciseMuscle);
}
