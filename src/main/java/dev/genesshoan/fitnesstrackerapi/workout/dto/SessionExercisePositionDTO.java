package dev.genesshoan.fitnesstrackerapi.workout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Exercise updated position dto")
public record SessionExercisePositionDTO(
        @Schema(description = "Exercise id") UUID exerciseId,

        @Schema(description = "Exercise new position") int position) {}
