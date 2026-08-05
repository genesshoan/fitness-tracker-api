package dev.genesshoan.fitnesstrackerapi.routine.mapper;

import dev.genesshoan.fitnesstrackerapi.exercise.mapper.ExerciseMapper;
import dev.genesshoan.fitnesstrackerapi.routine.domain.RoutineExercise;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineExerciseResponseDTO;
import org.mapstruct.Mapper;

@Mapper(
        componentModel = "spring",
        builder = @org.mapstruct.Builder(disableBuilder = true),
        uses = {ExerciseMapper.class})
public interface RoutineExerciseMapper {
    RoutineExerciseResponseDTO toRoutineExerciseResponseDTO(RoutineExercise routineExercise);
}
