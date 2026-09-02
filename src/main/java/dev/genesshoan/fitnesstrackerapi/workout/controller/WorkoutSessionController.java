package dev.genesshoan.fitnesstrackerapi.workout.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.workout.WorkoutSessionService;
import dev.genesshoan.fitnesstrackerapi.workout.dto.NotesUpdateRequestDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionListItemDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionRequestDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions")
public class WorkoutSessionController {

    private final WorkoutSessionService workoutSessionService;

    @Operation(
            summary = "Get all sessions",
            description = "Retrieve a paginated list of sessions for the authenticated user")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Sessions retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
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
                                schema = @Schema(implementation = ProblemDetail.class))),
    })
    @GetMapping
    public ResponseEntity<Page<WorkoutSessionListItemDTO>> getSessions(
            @AuthenticationPrincipal UserDetailsImpl principal, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(workoutSessionService.getAllWorkoutSessions(principal.getId(), pageable));
    }

    @Operation(summary = "Get session by id", description = "Retrieve a session by its id")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Session retrieved successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = WorkoutSessionResponseDTO.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Session not found",
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
    @GetMapping("/{sessionId}")
    public ResponseEntity<WorkoutSessionResponseDTO> getSession(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Workout session id") @PathVariable UUID sessionId) {
        return ResponseEntity.ok(workoutSessionService.getWorkoutSessionById(sessionId, principal.getId()));
    }

    @Operation(
            summary = "Create workout session from scratch",
            description = "Create a new session for the authenticated user")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Session created successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = WorkoutSessionResponseDTO.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request dto or exercise data",
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
                                schema = @Schema(implementation = ProblemDetail.class))),
    })
    @PostMapping
    public ResponseEntity<WorkoutSessionResponseDTO> createWorkoutSessionFromScratch(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody WorkoutSessionRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workoutSessionService.createWorkoutSessionFromScratch(requestDTO, principal.getId()));
    }

    @Operation(
            summary = "Create workout session from a routine",
            description = "Create a new session for the authenticated user")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Session created successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = WorkoutSessionResponseDTO.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid routine id",
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
                description = "Routine not found",
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
    @PostMapping("/from-routine/{routineId}")
    public ResponseEntity<WorkoutSessionResponseDTO> createWorkoutSessionFromRoutine(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Routine id") @PathVariable UUID routineId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workoutSessionService.createWorkoutSessionFromRoutine(routineId, principal.getId()));
    }

    @Operation(
            summary = "Update workout session notes",
            description = "Updates workout session notes for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Session notes updated successfully"),
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
                                schema = @Schema(implementation = ProblemDetail.class))),
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{sessionId}/notes")
    public void updateSessionNotes(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Workout session id") @PathVariable UUID sessionId,
            @Valid @RequestBody NotesUpdateRequestDTO requestDTO) {
        workoutSessionService.updateWorkoutSessionNotes(sessionId, principal.getId(), requestDTO);
    }

    @Operation(
            summary = "Complete workout session",
            description = "Complete workout session for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Session completed successfully"),
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
                                schema = @Schema(implementation = ProblemDetail.class))),
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{sessionId}/finish")
    public void completeWorkoutSession(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Workout session id") @PathVariable UUID sessionId) {
        workoutSessionService.completeWorkoutSession(sessionId, principal.getId());
    }

    @Operation(summary = "Delete workout session", description = "Delete workout session for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Session deleted successfully"),
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
                                schema = @Schema(implementation = ProblemDetail.class))),
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{sessionId}")
    public void deleteWorkoutSession(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Workout session id") @PathVariable UUID sessionId) {
        workoutSessionService.deleteWorkoutSession(sessionId, principal.getId());
    }
}
