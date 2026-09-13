package dev.genesshoan.fitnesstrackerapi.workout.mapper;

import dev.genesshoan.fitnesstrackerapi.stats.dto.AchievementDTO;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExerciseAddedResponseDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExercisePositionDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExerciseResponseDTO;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), uses = SessionSetMapper.class)
public interface SessionExerciseMapper {

    SessionExerciseResponseDTO toSessionExerciseResponseDTO(
            SessionExercise sessionExercise, @Context Map<UUID, List<AchievementDTO>> achievementsBySetId);

    @Named("withoutAchievements")
    default SessionExerciseResponseDTO toResponseWithoutAchievements(SessionExercise sessionExercise) {
        return toSessionExerciseResponseDTO(sessionExercise, Map.of());
    }

    @Mapping(source = "id", target = "exerciseId")
    SessionExercisePositionDTO toSessionExercisePositionDTO(SessionExercise sessionExercise);

    List<SessionExercisePositionDTO> toSessionExercisePositionDTOList(List<SessionExercise> exercises);

    @Mapping(target = "newExercise", qualifiedByName = "withoutAchievements")
    SessionExerciseAddedResponseDTO toSessionExerciseAddedResponseDTO(
            SessionExercise newExercise, List<SessionExercise> shiftedPositions);
}
