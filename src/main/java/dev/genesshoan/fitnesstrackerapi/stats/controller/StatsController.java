package dev.genesshoan.fitnesstrackerapi.stats.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.genesshoan.fitnesstrackerapi.common.annotation.CurrentUserId;
import dev.genesshoan.fitnesstrackerapi.stats.dto.ExerciseProgressPoints;
import dev.genesshoan.fitnesstrackerapi.stats.dto.MonthlyVolume;
import dev.genesshoan.fitnesstrackerapi.stats.dto.MuscleIntensityResponse;
import dev.genesshoan.fitnesstrackerapi.stats.dto.OneRepMax;
import dev.genesshoan.fitnesstrackerapi.stats.dto.SessionVolume;
import dev.genesshoan.fitnesstrackerapi.stats.dto.Streak;
import dev.genesshoan.fitnesstrackerapi.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * HTTP adapter for authenticated, user-scoped statistics queries.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stats")
@Tag(name = "Statistics", description = "Endpoints for retrieving workout statistics")
public class StatsController {

    private final StatsService statsService;

    @Operation(
            summary = "Get session volume",
            description = "Calculate completed strength volume for a workout session")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Session volume calculated successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = SessionVolume.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid session ID",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Workout session not found",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(value = "/volume", params = "sessionId")
    public ResponseEntity<SessionVolume> getSessionVolume(
            @CurrentUserId UUID userId,
            @Parameter(description = "Workout session id", required = true) @RequestParam UUID sessionId) {
        return ResponseEntity.ok(statsService.calculateSessionVolume(sessionId, userId));
    }

    /**
     * Returns completed strength volume for an inclusive local-date range.
     *
     * @param principal authenticated user
     * @param from inclusive start date in the user's timezone
     * @param to inclusive end date in the user's timezone
     * @return total volume in kilogram-repetitions
     */
    @Operation(
            summary = "Get volume for a date range",
            description = "Calculate completed strength volume in the user's timezone")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Date-range volume calculated successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = SessionVolume.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid date range",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(
            value = "/volume",
            params = {"from", "to"})
    public ResponseEntity<SessionVolume> getVolume(
            @CurrentUserId UUID userId,
            @Parameter(
                            description = "Inclusive start date in the user's timezone",
                            required = true,
                            example = "2026-01-01")
                    @RequestParam
                    LocalDate from,
            @Parameter(
                            description = "Inclusive end date in the user's timezone",
                            required = true,
                            example = "2026-01-31")
                    @RequestParam
                    LocalDate to) {
        return ResponseEntity.ok(statsService.calculateVolume(userId, from, to));
    }

    /**
     * Returns completed strength volume grouped by calendar month.
     *
     * @param principal authenticated user
     * @param from inclusive start date in the user's timezone
     * @param to inclusive end date in the user's timezone
     * @return chronological monthly volume
     */
    @Operation(
            summary = "Get monthly volume",
            description = "Calculate completed strength volume grouped by calendar month in the user's timezone")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Monthly volume calculated successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                array = @ArraySchema(schema = @Schema(implementation = MonthlyVolume.class)))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid date range",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/volume/monthly")
    public ResponseEntity<List<MonthlyVolume>> getMonthlyVolume(
            @CurrentUserId UUID userId,
            @Parameter(
                            description = "Inclusive start date in the user's timezone",
                            required = true,
                            example = "2026-01-01")
                    @RequestParam
                    LocalDate from,
            @Parameter(
                            description = "Inclusive end date in the user's timezone",
                            required = true,
                            example = "2026-12-31")
                    @RequestParam
                    LocalDate to) {
        return ResponseEntity.ok(statsService.getMonthlyVolume(userId, from, to));
    }

    @Operation(
            summary = "Get estimated one-repetition maximum",
            description = "Get the user's highest estimated one-repetition maximum for an exercise")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Estimated one-repetition maximum retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = OneRepMax.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid exercise ID",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Exercise not found",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/1rm")
    public ResponseEntity<OneRepMax> getOneRepMax(
            @CurrentUserId UUID userId,
            @Parameter(description = "Exercise id", required = true) @RequestParam UUID exerciseId) {
        return ResponseEntity.ok(statsService.getOneRepMax(userId, exerciseId));
    }

    @Operation(
            summary = "Get current workout streak",
            description = "Get the authenticated user's current and longest workout streak")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Workout streak retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Streak.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/streak")
    public ResponseEntity<Streak> getCurrentStreak(@CurrentUserId UUID userId) {
        return ResponseEntity.ok(statsService.getCurrentStreak(userId, Instant.now()));
    }

    @Operation(
            summary = "Get exercise progression",
            description = "Get chronological performance points for an exercise in a date range")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Exercise progression retrieved successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ExerciseProgressPoints.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid exercise ID or date range",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Exercise not found",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/progress")
    public ResponseEntity<ExerciseProgressPoints> getExerciseProgress(
            @CurrentUserId UUID userId,
            @Parameter(description = "Exercise id", required = true) @RequestParam UUID exerciseId,
            @Parameter(
                            description = "Inclusive start of the date range in ISO-8601 format",
                            required = true,
                            example = "2026-01-01T00:00:00Z")
                    @RequestParam
                    Instant from,
            @Parameter(
                            description = "Exclusive end of the date range in ISO-8601 format",
                            required = true,
                            example = "2026-02-01T00:00:00Z")
                    @RequestParam
                    Instant to) {
        return ResponseEntity.ok(statsService.getExerciseProgress(userId, exerciseId, from, to));
    }

    @Operation(
            summary = "Get muscle intensity",
            description = "Get deterministic range-relative application intensity for every muscle. "
                    + "Scores are a visualization heuristic, not physiological muscle activation.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Muscle intensity calculated successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = MuscleIntensityResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid date range",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/muscle-intensity")
    public ResponseEntity<MuscleIntensityResponse> getMuscleIntensity(
            @CurrentUserId UUID userId,
            @Parameter(
                            description = "Inclusive start of the date range in the user's timezone",
                            required = true,
                            example = "2026-09-01")
                    @RequestParam
                    LocalDate from,
            @Parameter(
                            description = "Inclusive end of the date range in the user's timezone",
                            required = true,
                            example = "2026-09-30")
                    @RequestParam
                    LocalDate to) {
        return ResponseEntity.ok(statsService.getMuscleIntensity(userId, from, to));
    }
}
