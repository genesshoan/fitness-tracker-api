package dev.genesshoan.fitnesstrackerapi.workout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Session exercise added to workout session response dto")
public record SessionExerciseAddedResponseDTO(
        @Schema(description = "Added session exercise") SessionExerciseResponseDTO newExercise,

        @Schema(description = "Session exercises updated positions")
        List<SessionExercisePositionDTO> shiftedPositions) {}
