package dev.genesshoan.fitnesstrackerapi.workout.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web.ExerciseMapper;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExerciseAddedResponse;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExercisePosition;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExerciseResponse;
import dev.genesshoan.fitnesstrackerapi.workout.dto.UnlockedAchievement;
import org.mapstruct.Builder;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        uses = {SessionSetMapper.class, ExerciseMapper.class})
public interface SessionExerciseMapper {

    SessionExerciseResponse toSessionExerciseResponse(
            SessionExercise sessionExercise, @Context Map<UUID, List<UnlockedAchievement>> achievementsBySetId);

    @Named("withoutAchievements")
    default SessionExerciseResponse toResponseWithoutAchievements(SessionExercise sessionExercise) {
        return toSessionExerciseResponse(sessionExercise, Map.of());
    }

    @Mapping(source = "id", target = "exerciseId")
    SessionExercisePosition toSessionExercisePosition(SessionExercise sessionExercise);

    List<SessionExercisePosition> toSessionExercisePositionList(List<SessionExercise> exercises);

    @Mapping(target = "newExercise", qualifiedByName = "withoutAchievements")
    SessionExerciseAddedResponse toSessionExerciseAddedResponse(
            SessionExercise newExercise, List<SessionExercise> shiftedPositions);
}
