package dev.genesshoan.fitnesstrackerapi.exercise;

import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPage;
import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPageRequest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseDetailDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseListItemDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/exercises")
@Tag(
    name = "Exercises",
    description = "Endpoints for managing and retrieving exercises"
)
public class ExerciseController {

    private final ExerciseService exerciseService;

    @Operation(
        summary = "Get all exercises",
        description = "Retrieve a paginated list of all exercises"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Exercise retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CursorPage.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
    })
    @GetMapping
    public ResponseEntity<
        CursorPage<ExerciseListItemDTO, UUID>
    > getAllExercises(
        @Parameter(
            description = "Pagination cursor for next page"
        ) @RequestParam(required = false) UUID cursor,
        @Parameter(description = "Number of items to return") @RequestParam(
            required = false
        ) @Max(100) @Positive Integer size,
        @Parameter(description = "Category of the exercise") @RequestParam(
            required = false
        ) Category category,
        @Parameter(
            description = "Difficulty level of the exercise"
        ) @RequestParam(required = false) Difficulty difficulty,
        @Parameter(
            description = "List of muscle slugs to filter by"
        ) @RequestParam(required = false) List<String> muscleSlugs
    ) {
        var request = new CursorPageRequest<>(cursor, size);

        return ResponseEntity.ok(
            exerciseService.getAllExercises(
                request,
                category,
                difficulty,
                muscleSlugs
            )
        );
    }

    @Operation(
        summary = "Get exercise by slug",
        description = "Retrieve an exercise by its slug"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Exercise retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ExerciseDetailDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Exercise not found",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
    })
    @GetMapping("/{slug}")
    public ResponseEntity<ExerciseDetailDTO> getExerciseBySlug(
        @Parameter(
            description = "Slug of the exercise"
        ) @PathVariable @NotBlank String slug
    ) {
        return ResponseEntity.ok(exerciseService.getExerciseBySlug(slug));
    }
}
