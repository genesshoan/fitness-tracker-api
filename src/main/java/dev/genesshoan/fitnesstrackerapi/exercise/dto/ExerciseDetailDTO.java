package dev.genesshoan.fitnesstrackerapi.exercise.dto;

import java.util.List;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Detailed exercise response, including associated muscles and impact levels.
 *
 * @param id exercise identifier
 * @param name exercise display name
 * @param slug URL-friendly exercise identifier
 * @param description exercise description
 * @param instructions ordered execution instructions
 * @param category exercise category
 * @param difficulty exercise difficulty
 * @param exerciseMuscles associated muscles
 * @param gifUrl client-facing media URL, currently a placeholder
 */
@Schema(description = "Exercise detail DTO")
public record ExerciseDetailDTO(
        @Schema(description = "Exercise ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Exercise name", example = "Bicep Curl")
        String name,

        @Schema(description = "Exercise slug", example = "bicep-curl")
        String slug,

        @Schema(description = "Exercise description", example = "Made with a bicep curl bar")
        String description,

        @Schema(description = "Step-by-step instructions for performing the exercise")
        List<String> instructions,

        @Schema(description = "Exercise category", example = "ARM")
        Category category,

        @Schema(description = "Exercise difficulty", example = "INTERMEDIATE")
        Difficulty difficulty,

        @Schema(description = "Exercise muscles") List<ExerciseMuscleDTO> exerciseMuscles,

        @Schema(description = "URL to the exercise GIF (placeholder, always null for now)")
        String gifUrl) {}
