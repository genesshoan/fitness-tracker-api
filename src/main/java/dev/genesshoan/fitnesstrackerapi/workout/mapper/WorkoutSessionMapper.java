package dev.genesshoan.fitnesstrackerapi.workout.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.routine.mapper.RoutineMapper;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import dev.genesshoan.fitnesstrackerapi.workout.dto.UnlockedAchievementDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionListItemDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionResponseDTO;
import org.mapstruct.Builder;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        uses = {SessionExerciseMapper.class, RoutineMapper.class})
public interface WorkoutSessionMapper {

    @Mapping(target = "achievements", source = "unlockedAchievements")
    WorkoutSessionResponseDTO toWorkoutSessionResponseDTO(
            WorkoutSession workoutSession,
            List<UnlockedAchievementDTO> unlockedAchievements,
            @Context Map<UUID, List<UnlockedAchievementDTO>> achievementsBySetId);

    WorkoutSessionListItemDTO toWorkoutSessionListItemDTO(WorkoutSession workoutSession);
}
