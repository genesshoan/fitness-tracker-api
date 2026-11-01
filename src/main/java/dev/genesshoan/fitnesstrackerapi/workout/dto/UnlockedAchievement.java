package dev.genesshoan.fitnesstrackerapi.workout.dto;

import java.time.Instant;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementTier;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An achievement that a training event unlocked.
 *
 * @param id the identifier of the user achievement
 * @param code the stable key of the achievement
 * @param name the display name of the achievement
 * @param tier the visual rarity of the achievement
 * @param exerciseId the exercise the achievement is scoped to, or {@code null}
 *        for a global achievement
 * @param unlockedAt the instant the achievement was unlocked
 */
@Schema(description = "Achievement unlocked by the training event")
public record UnlockedAchievementDTO(
        @Schema(description = "User achievement id") UUID id,

        @Schema(description = "Achievement code", example = "PR_MAX_WEIGHT_PER_EXERCISE")
        String code,

        @Schema(description = "Achievement name", example = "Weight record")
        String name,

        @Schema(description = "Achievement tier", example = "SILVER")
        AchievementTier tier,

        @Schema(description = "Exercise the achievement is scoped to", nullable = true)
        UUID exerciseId,

        @Schema(description = "Instant the achievement was unlocked")
        Instant unlockedAt) {}
