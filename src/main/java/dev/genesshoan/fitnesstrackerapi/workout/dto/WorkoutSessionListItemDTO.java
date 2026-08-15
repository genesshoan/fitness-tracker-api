package dev.genesshoan.fitnesstrackerapi.workout.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineListItemDTO;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Workout session list item DTO")
public record WorkoutSessionListItemDTO(
        @Schema(description = "Workout session ID")
        UUID id,

        @Schema(description = "Session status", example = "COMPLETED")
        SessionStatus status,

        @Schema(description = "Date and time when the session was created")
        LocalDateTime createdAt,

        @Schema(description = "Date and time when the session was completed")
        Instant completedAt,

        @Schema(description = "Routine used for the workout session")
        RoutineListItemDTO routine
) {}
