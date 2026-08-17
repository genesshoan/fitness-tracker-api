package dev.genesshoan.fitnesstrackerapi.workout.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Workout session request dto")
public record WorkoutSessionRequestDTO(
        @NotNull @Schema(description = "Workout session current status", example = "IN_PROGRESS")
        SessionStatus status,

        @Schema(description = "Date tima the session was completed")
        Instant completedAt,

        @Schema(description = "Workout session notes") String notes,

        @Valid @Schema(description = "Exercise performed for the workout session")
        List<SessionExerciseRequestDTO> exercises) {}
