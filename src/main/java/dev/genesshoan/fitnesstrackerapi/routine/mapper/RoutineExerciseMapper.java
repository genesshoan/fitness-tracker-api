package dev.genesshoan.fitnesstrackerapi.routine.mapper;

import dev.genesshoan.fitnesstrackerapi.exercise.mapper.ExerciseMapper;
import dev.genesshoan.fitnesstrackerapi.routine.domain.RoutineExercise;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineExerciseResponseDTO;
import org.mapstruct.Mapper;

/** Maps routine exercise entities to API response DTOs. */
@Mapper(
        componentModel = "spring",
        builder = @org.mapstruct.Builder(disableBuilder = true),
        uses = {ExerciseMapper.class})
public interface RoutineExerciseMapper {
    /**
     * Maps an assignment and its referenced exercise to its response shape.
     *
     * @param routineExercise the entity to map
     * @return the routine exercise response
     */
    RoutineExerciseResponseDTO toRoutineExerciseResponseDTO(RoutineExercise routineExercise);
}
