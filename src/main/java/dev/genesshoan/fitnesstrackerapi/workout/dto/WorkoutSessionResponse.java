package dev.genesshoan.fitnesstrackerapi.workout.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.common.domain.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Workout session response dto")
public record WorkoutSessionResponse(
        @Schema(description = "Session workout id") UUID id,

        @Schema(description = "Workout session current status", example = "IN_PROGRESS")
        SessionStatus status,

        @Schema(description = "Data time the session started")
        Instant startedAt,

        @Schema(description = "Date tima the session was completed")
        Instant completedAt,

        @Schema(description = "Workout session notes") String notes,

        @Schema(description = "Exercise performed for the workout session")
        List<SessionExerciseResponse> exercises,

        @Schema(description = "Achievements unlocked by the event that produced this response")
        List<UnlockedAchievement> achievements) {}
