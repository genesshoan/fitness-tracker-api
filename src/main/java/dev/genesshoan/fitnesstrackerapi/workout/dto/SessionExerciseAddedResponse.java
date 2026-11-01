package dev.genesshoan.fitnesstrackerapi.workout.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Session exercise added to workout session response dto")
public record SessionExerciseAddedResponse(
        @Schema(description = "Added session exercise") SessionExerciseResponse newExercise,

        @Schema(description = "Session exercises updated positions")
        List<SessionExercisePosition> shiftedPositions) {}
