package dev.genesshoan.fitnesstrackerapi.workout.mapper;

import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionResponseDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), uses = SessionExerciseMapper.class)
public interface WorkoutSessionMapper {

    WorkoutSessionResponseDTO toWorkoutSessionResponseDTO(WorkoutSession workoutSession);
}
