package dev.genesshoan.fitnesstrackerapi.workout.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.workout.WorkoutSessionService;
import dev.genesshoan.fitnesstrackerapi.workout.dto.NotesUpdateRequestDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.PositionRequestDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExerciseAddedResponseDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExercisePositionDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExerciseRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions/{sessionId}/exercises")
public class SessionExerciseController {

    private final WorkoutSessionService workoutSessionService;

    @Operation(
            summary = "Add session exercise",
            description = "Add a new exercise to a workout session for the authenticated user")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Session exercise added successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = SessionExerciseAddedResponseDTO.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request dto, workout session is already completed, or exercise set data",
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
    public ResponseEntity<SessionExerciseAddedResponseDTO> addSessionExercise(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Workout session id") @PathVariable UUID sessionId,
            @Valid @RequestBody SessionExerciseRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workoutSessionService.addNewSessionExercise(sessionId, principal.getId(), requestDTO));
    }

    @Operation(
            summary = "Update session exercise notes",
            description = "Update session exercise notes for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Session exercise notes updated successfully"),
        @ApiResponse(
                responseCode = "400",
                description = "Workout session is already completed",
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{sessionExerciseId}/notes")
    public void updateSessionExerciseNotes(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Workout session id") @PathVariable UUID sessionId,
            @Parameter(description = "Session exercise id") @PathVariable UUID sessionExerciseId,
            @Valid @RequestBody NotesUpdateRequestDTO requestDTO) {
        workoutSessionService.updateSessionExerciseNotes(sessionId, sessionExerciseId, principal.getId(), requestDTO);
    }

    @Operation(
            summary = "Update session exercise position",
            description = "Update session exercise position for the authenticated user")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Session exercise position updated successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                array =
                                        @ArraySchema(
                                                schema = @Schema(implementation = SessionExercisePositionDTO.class)))),
        @ApiResponse(
                responseCode = "400",
                description = "Workout session is already completed",
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
    @PatchMapping("/{sessionExerciseId}/position")
    public ResponseEntity<List<SessionExercisePositionDTO>> updateSessionExercisePosition(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Workout session id") @PathVariable UUID sessionId,
            @Parameter(description = "Session exercise id") @PathVariable UUID sessionExerciseId,
            @Valid @RequestBody PositionRequestDTO requestDTO) {
        return ResponseEntity.ok(workoutSessionService.updateSessionExercisePosition(
                sessionId, sessionExerciseId, principal.getId(), requestDTO));
    }

    @Operation(
            summary = "Delete session exercise",
            description = "Delete session exercise from a workout session for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Session exercise deleted successfully"),
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{sessionExerciseId}")
    public void deleteSessionExercise(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Workout session id") @PathVariable UUID sessionId,
            @Parameter(description = "Session exercise id") @PathVariable UUID sessionExerciseId) {
        workoutSessionService.deleteSessionExercise(sessionId, sessionExerciseId, principal.getId());
    }
}
