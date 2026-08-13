package dev.genesshoan.fitnesstrackerapi.workout.dto;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Workout session response dto")
public record WorkoutSessionResponseDTO(
        @Schema(description = "Session workout id")
        UUID id,

        @Schema(description = "Workout session current status", example = "IN_PROGRESS")
        SessionStatus status,

        @Schema(description = "Date tima the session was completed")
        LocalDateTime completedAt,

        @Schema(description = "Workout session notes") String notes,

        @Schema(description = "Data version") int version,

        @Schema(description = "Exercise performed for the workout session")
        List<SessionExerciseResponseDTO> exercises) {}
