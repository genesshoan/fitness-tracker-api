package dev.genesshoan.fitnesstrackerapi.routine;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineExerciseRequestDTO;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineListItemDTO;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineRequestDTO;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineResponseDTO;
import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/routines")
@Tag(name = "Routines", description = "Endpoints for managing and retrieving routines")
public class RoutineController {

    private final RoutineService routineService;

    @Operation(
            summary = "Get all routines",
            description = "Retrieve a paginated list of routines for the authenticated user")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Routines retrieved successfully",
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
    public ResponseEntity<Page<RoutineListItemDTO>> getRoutines(
            @AuthenticationPrincipal UserDetailsImpl principal, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(routineService.getRoutinesByUserId(principal.getUser(), pageable));
    }

    @Operation(summary = "Get routine by id", description = "Retrieve a routine by its id")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Routine retrieved successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = RoutineResponseDTO.class))),
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
    @GetMapping("/{routineId}")
    public ResponseEntity<RoutineResponseDTO> getRoutine(
            @Parameter(description = "Routine id") @PathVariable UUID routineId,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(routineService.getRoutineById(routineId, principal.getUser()));
    }

    @Operation(summary = "Create routine", description = "Create a new routine for the authenticated user")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Routine created successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = RoutineResponseDTO.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request parameters",
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
                responseCode = "409",
                description = "Routine with the same name already exists",
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
    public ResponseEntity<RoutineResponseDTO> createRoutine(
            @Valid @RequestBody RoutineRequestDTO requestDTO, @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routineService.createRoutine(requestDTO, principal.getUser()));
    }

    @Operation(summary = "Update routine", description = "Update an existing routine")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Routine updated successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = RoutineResponseDTO.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request parameters",
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
                responseCode = "409",
                description = "Routine with the same name already exists",
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
    @PutMapping("/{routineId}")
    public ResponseEntity<RoutineResponseDTO> updateRoutine(
            @Parameter(description = "Routine id") @PathVariable UUID routineId,
            @Valid @RequestBody RoutineRequestDTO requestDTO,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(routineService.updateRoutine(routineId, requestDTO, principal.getUser()));
    }

    @Operation(summary = "Delete routine", description = "Soft delete a routine")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Routine deleted successfully"),
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{routineId}")
    public ResponseEntity<Void> deleteRoutine(
            @Parameter(description = "Routine id") @PathVariable UUID routineId,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        routineService.deleteRoutine(routineId, principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Add exercise to routine",
            description = "Add an exercise at a specific position in the routine")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Exercise added successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = RoutineResponseDTO.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request parameters",
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
                description = "Routine or exercise not found",
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
    @PostMapping("/{routineId}/exercises/{position}")
    public ResponseEntity<RoutineResponseDTO> addRoutineExercise(
            @Parameter(description = "Routine id") @PathVariable UUID routineId,
            @Parameter(description = "Position to insert the exercise at") @PathVariable @Min(1) Integer position,
            @Valid @RequestBody RoutineExerciseRequestDTO requestDTO,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(
                routineService.addRoutineExercise(routineId, position, requestDTO, principal.getUser()));
    }

    @Operation(
            summary = "Delete exercise from routine",
            description = "Remove an exercise at a specific position from the routine")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Exercise removed successfully"),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Routine or exercise not found",
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
    @DeleteMapping("/{routineId}/exercises/{position}")
    public ResponseEntity<Void> deleteRoutineExercise(
            @Parameter(description = "Routine id") @PathVariable UUID routineId,
            @Parameter(description = "Position of the exercise to remove") @PathVariable @Min(1) Integer position,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        routineService.deleteRoutineExercise(routineId, position, principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Update exercise in routine",
            description = "Update an exercise at a specific position in the routine")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Exercise updated successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = RoutineResponseDTO.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request parameters",
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
                description = "Routine or exercise not found",
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
    @PutMapping("/{routineId}/exercises/{position}")
    public ResponseEntity<RoutineResponseDTO> updateRoutineExercise(
            @Parameter(description = "Routine id") @PathVariable UUID routineId,
            @Parameter(description = "Position of the exercise to update") @PathVariable @Min(1) Integer position,
            @Valid @RequestBody RoutineExerciseRequestDTO requestDTO,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(
                routineService.updateRoutineExercise(routineId, position, requestDTO, principal.getUser()));
    }
}
