package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An achievement the user has not unlocked yet.
 *
 * @param code the stable key of the achievement
 * @param name the display name of the achievement
 * @param progressValue the best value reached towards the threshold
 * @param threshold the value the metric has to reach
 * @param remaining how much is missing to reach the threshold
 */
@Schema(description = "Achievement the user has not unlocked yet")
public record NextAchievementDTO(
        @Schema(description = "Achievement code", example = "WORKOUTS_10")
        String code,

        @Schema(description = "Achievement name", example = "Ten workouts")
        String name,

        @Schema(description = "Best value reached towards the threshold", example = "7", nullable = true)
        Double progressValue,

        @Schema(description = "Value the metric has to reach", example = "10", nullable = true)
        Double threshold,

        @Schema(description = "How much is missing to reach the threshold", example = "3", nullable = true)
        Double remaining) {}
