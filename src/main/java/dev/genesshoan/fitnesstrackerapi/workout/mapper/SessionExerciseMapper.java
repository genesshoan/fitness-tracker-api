package dev.genesshoan.fitnesstrackerapi.workout.mapper;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExerciseResponseDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), uses = SessionSetMapper.class)
public interface SessionExerciseMapper {

    SessionExerciseResponseDTO toSessionExerciseResponseDTO(SessionExercise sessionExercise);
}
