package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementCategory;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementTier;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An achievement the user unlocked.
 *
 * @param id the identifier of the state of the user
 * @param code the stable key of the achievement
 * @param name the display name of the achievement
 * @param tier the visual rarity of the achievement
 * @param category the subject the achievement belongs to
 * @param exerciseId the exercise the achievement is scoped to
 * @param exerciseName the name of the exercise the achievement is scoped to
 * @param unlockedAt the instant the achievement was unlocked
 * @param seen whether the user has seen the unlock
 */
@Schema(description = "Achievement unlocked by the user")
public record AchievementFeedResponse(
        @Schema(description = "Identifier of the state of the user")
        UUID id,

        @Schema(description = "Achievement code", example = "PR_MAX_WEIGHT_PER_EXERCISE")
        String code,

        @Schema(description = "Achievement name", example = "Weight record")
        String name,

        @Schema(description = "Rarity of the achievement", example = "SILVER", nullable = true)
        AchievementTier tier,

        @Schema(description = "Category of the achievement", example = "PERSONAL_RECORD", nullable = true)
        AchievementCategory category,

        @Schema(description = "Exercise the achievement is scoped to", nullable = true)
        UUID exerciseId,

        @Schema(
                description = "Name of the exercise the achievement is scoped to",
                example = "Bench press",
                nullable = true)
        String exerciseName,

        @Schema(description = "Instant the achievement was unlocked")
        Instant unlockedAt,

        @Schema(description = "Whether the user has seen the unlock")
        boolean seen) {}
