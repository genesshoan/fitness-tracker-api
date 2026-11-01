package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The unlocked and total counts of one category.
 *
 * @param unlocked the number of achievements the user unlocked
 * @param total the number of achievements in the category
 */
@Schema(description = "Unlocked and total counts of one category")
public record AchievementCategorySummary(
        @Schema(description = "Number of achievements the user unlocked", example = "3")
        long unlocked,

        @Schema(description = "Number of achievements in the category", example = "5")
        long total) {}
