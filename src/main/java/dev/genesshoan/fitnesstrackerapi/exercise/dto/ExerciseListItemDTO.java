package dev.genesshoan.fitnesstrackerapi.exercise.dto;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Exercise list item DTO")
public record ExerciseListItemDTO(
    @Schema(
        description = "Exercise ID",
        example = "123e4567-e89b-12d3-a456-426614174000"
    )
    UUID id,
    @Schema(description = "Exercise name", example = "Bicep Curl") String name,
    @Schema(description = "Exercise slug", example = "bicep-curl") String slug,
    @Schema(description = "Exercise category", example = "ARM")
    Category category,
    @Schema(description = "Exercise difficulty", example = "INTERMEDIATE")
    Difficulty difficulty
) {}
