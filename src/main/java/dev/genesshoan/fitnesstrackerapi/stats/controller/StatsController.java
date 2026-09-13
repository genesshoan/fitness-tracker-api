package dev.genesshoan.fitnesstrackerapi.stats.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.stats.dto.ExerciseProgressPointsDTO;
import dev.genesshoan.fitnesstrackerapi.stats.dto.OneRepMaxDTO;
import dev.genesshoan.fitnesstrackerapi.stats.dto.SessionVolumeDTO;
import dev.genesshoan.fitnesstrackerapi.stats.dto.StreakDTO;
import dev.genesshoan.fitnesstrackerapi.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

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
                                schema = @Schema(implementation = SessionVolumeDTO.class))),
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
    @GetMapping("/volume")
    public ResponseEntity<SessionVolumeDTO> getSessionVolume(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Workout session id", required = true) @RequestParam UUID sessionId) {
        return ResponseEntity.ok(statsService.calculateSessionVolume(sessionId, principal.getId()));
    }

    @Operation(
            summary = "Get estimated one-repetition maximum",
            description = "Get the user's highest estimated one-repetition maximum for an exercise")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Estimated one-repetition maximum retrieved successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = OneRepMaxDTO.class))),
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
    public ResponseEntity<OneRepMaxDTO> getOneRepMax(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Exercise id", required = true) @RequestParam UUID exerciseId) {
        return ResponseEntity.ok(statsService.getOneRepMax(principal.getId(), exerciseId));
    }

    @Operation(
            summary = "Get current workout streak",
            description = "Get the authenticated user's current and longest workout streak")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Workout streak retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = StreakDTO.class))),
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
    public ResponseEntity<StreakDTO> getCurrentStreak(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(statsService.getCurrentStreak(
                principal.getId(), Instant.now(), principal.getUser().getTimezone()));
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
                                schema = @Schema(implementation = ExerciseProgressPointsDTO.class))),
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
    public ResponseEntity<ExerciseProgressPointsDTO> getExerciseProgress(
            @AuthenticationPrincipal UserDetailsImpl principal,
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
        return ResponseEntity.ok(statsService.getExerciseProgress(
                principal.getId(), exerciseId, from, to, principal.getUser().getTimezone()));
    }
}
