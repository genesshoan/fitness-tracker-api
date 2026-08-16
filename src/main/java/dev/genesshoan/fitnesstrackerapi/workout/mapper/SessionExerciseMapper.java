package dev.genesshoan.fitnesstrackerapi.workout.mapper;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExerciseAddedResponseDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExercisePositionDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExerciseResponseDTO;
import java.util.List;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), uses = SessionSetMapper.class)
public interface SessionExerciseMapper {

    SessionExerciseResponseDTO toSessionExerciseResponseDTO(SessionExercise sessionExercise);

    @Mapping(source = "id", target = "exerciseId")
    SessionExercisePositionDTO toSessionExercisePositionDTO(SessionExercise sessionExercise);

    List<SessionExercisePositionDTO> toSessionExercisePositionDTOList(List<SessionExercise> exercises);

    SessionExerciseAddedResponseDTO toSessionExerciseAddedResponseDTO(
            SessionExercise newExercise, List<SessionExercise> shiftedPositions);
}
