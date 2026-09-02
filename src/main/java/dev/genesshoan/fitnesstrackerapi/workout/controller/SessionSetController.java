package dev.genesshoan.fitnesstrackerapi.workout.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.workout.WorkoutSessionService;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionSetRequestDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionSetResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/sets")
public class SessionSetController {

    private final WorkoutSessionService workoutSessionService;

    @Operation(
            summary = "Add session set",
            description = "Add a new set to a workout session exercise for the authenticated user")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Session set added successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = SessionSetResponseDTO.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request dto, workout session is already completed, or exercise category data",
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
                description = "Workout session or exercise not found",
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
                                schema = @Schema(implementation = ProblemDetail.class))),
    })
    @PostMapping
    public ResponseEntity<SessionSetResponseDTO> addSessionSet(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Workout session id") @PathVariable UUID sessionId,
            @Parameter(description = "Session exercise id") @PathVariable UUID sessionExerciseId,
            @Valid @RequestBody SessionSetRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workoutSessionService.addNewSessionSet(
                        sessionId, sessionExerciseId, principal.getId(), requestDTO));
    }

    @Operation(
            summary = "Update session set",
            description = "Update session set from a workout session exercise for the authenticated user")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Session set updated successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = SessionSetResponseDTO.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Workout session is already completed or set data is invalid for the exercise category",
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
                description = "Workout session, exercise or set not found",
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
                                schema = @Schema(implementation = ProblemDetail.class))),
    })
    @PutMapping("/{sessionSetId}")
    public ResponseEntity<SessionSetResponseDTO> updateSessionSet(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Workout session id") @PathVariable UUID sessionId,
            @Parameter(description = "Session exercise id") @PathVariable UUID sessionExerciseId,
            @Parameter(description = "Session set id") @PathVariable UUID sessionSetId,
            @Valid @RequestBody SessionSetRequestDTO requestDTO) {
        return ResponseEntity.ok(workoutSessionService.updateSessionSet(
                sessionId, sessionExerciseId, sessionSetId, principal.getId(), requestDTO));
    }

    @Operation(
            summary = "Delete session set",
            description = "Delete session set from a workout session exercise for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Session set deleted successfully"),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Workout session is already completed",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Workout session, exercise or set not found",
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
                                schema = @Schema(implementation = ProblemDetail.class))),
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{sessionSetId}")
    public void deleteSessionSet(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Workout session id") @PathVariable UUID sessionId,
            @Parameter(description = "Session exercise id") @PathVariable UUID sessionExerciseId,
            @Parameter(description = "Session set id") @PathVariable UUID sessionSetId) {
        workoutSessionService.deleteSessionSet(sessionId, sessionExerciseId, sessionSetId, principal.getId());
    }
}
