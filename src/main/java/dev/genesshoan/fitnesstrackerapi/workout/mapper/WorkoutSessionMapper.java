package dev.genesshoan.fitnesstrackerapi.workout.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.routine.mapper.RoutineMapper;
import dev.genesshoan.fitnesstrackerapi.stats.dto.AchievementDTO;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionListItemDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionResponseDTO;
import org.mapstruct.Builder;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        uses = {SessionExerciseMapper.class, RoutineMapper.class})
public interface WorkoutSessionMapper {

    WorkoutSessionResponseDTO toWorkoutSessionResponseDTO(
            WorkoutSession workoutSession, @Context Map<UUID, List<AchievementDTO>> achievementsBySetId);

    WorkoutSessionListItemDTO toWorkoutSessionListItemDTO(WorkoutSession workoutSession);
}
