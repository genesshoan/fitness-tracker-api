package dev.genesshoan.fitnesstrackerapi.workout.dto;

import java.time.Instant;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.common.domain.SessionStatus;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineListItem;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Workout session list item")
public record WorkoutSessionListItem(
        @Schema(description = "Workout session ID") UUID id,

        @Schema(description = "Session status", example = "COMPLETED")
        SessionStatus status,

        @Schema(description = "Date and time when the session was created")
        Instant createdAt,

        @Schema(description = "Date and time when the session was completed")
        Instant completedAt,

        @Schema(description = "Number of exercises in the workout session")
        int exerciseCount,

        @Schema(description = "Routine used for the workout session")
        RoutineListItem routine) {}
