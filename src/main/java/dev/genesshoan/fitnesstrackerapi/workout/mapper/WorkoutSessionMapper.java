package dev.genesshoan.fitnesstrackerapi.workout.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.routine.mapper.RoutineMapper;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import dev.genesshoan.fitnesstrackerapi.workout.dto.UnlockedAchievement;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionListItem;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionResponse;
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
    WorkoutSessionResponse toWorkoutSessionResponse(
            WorkoutSession workoutSession,
            List<UnlockedAchievement> unlockedAchievements,
            @Context Map<UUID, List<UnlockedAchievement>> achievementsBySetId);

    WorkoutSessionListItem toWorkoutSessionListItem(WorkoutSession workoutSession);
}
