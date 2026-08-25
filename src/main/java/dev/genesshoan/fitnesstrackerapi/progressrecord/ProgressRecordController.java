package dev.genesshoan.fitnesstrackerapi.progressrecord;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.genesshoan.fitnesstrackerapi.progressrecord.dto.ProgressRecordRequestDTO;
import dev.genesshoan.fitnesstrackerapi.progressrecord.dto.ProgressRecordResponseDTO;
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
@RequestMapping("/api/v1/progress")
@Tag(name = "Progress Record", description = "Endpoints for managing and retrieving progress records")
public class ProgressRecordController {

    private final ProgressRecordService progressRecordService;

    @Operation(summary = "Get progress records", description = "Retrieve progress records within a date range")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Progress records retrieved successfully"),
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
    @GetMapping
    public ResponseEntity<Page<ProgressRecordResponseDTO>> getProgressRecordsInDateRange(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Start date of the range") @RequestParam @NotNull LocalDate from,
            @Parameter(description = "End date of the range") @RequestParam @NotNull LocalDate to,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(
                progressRecordService.getProgressRecordsInDateRange(principal.getId(), from, to, pageable));
    }

    @Operation(
            summary = "Create progress record",
            description = "Create a new progress record for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Progress record created successfully"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid progress record data",
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
                description = "A progress record already exists for this date",
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
    @PostMapping
    public ResponseEntity<ProgressRecordResponseDTO> createProgressRecord(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody ProgressRecordRequestDTO requestDTO) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(progressRecordService.createProgressRecord(principal.getId(), requestDTO));
    }

    @Operation(
            summary = "Delete progress record",
            description = "Delete a progress record belonging to the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Progress record deleted successfully"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid progress record ID",
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
                description = "Progress record not found",
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
    @DeleteMapping("/{id}")
    public void deleteProgressRecord(
            @AuthenticationPrincipal UserDetailsImpl principal, @PathVariable @NotNull UUID progressRecordId) {
        progressRecordService.deleteProgressRecord(progressRecordId, principal.getId());
    }
}
