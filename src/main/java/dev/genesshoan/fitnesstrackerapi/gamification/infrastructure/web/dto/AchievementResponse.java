package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementCategory;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementMetric;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementScope;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementTier;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.RuleType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An achievement of the catalog with the state the user reached in it.
 *
 * @param code the stable key of the achievement
 * @param name the display name of the achievement
 * @param description what the user has to do to earn the achievement
 * @param category the subject the achievement belongs to
 * @param tier the visual rarity of the achievement
 * @param ruleType the kind of rule that unlocks the achievement
 * @param metric the metric the achievement is measured against
 * @param threshold the value the metric has to reach
 * @param scope how many badges the rule produces
 * @param iconKey the key of the image that represents the achievement
 * @param userAchievementId the identifier of the state of the user
 * @param exerciseId the exercise the achievement is scoped to
 * @param exerciseName the name of the exercise the achievement is scoped to
 * @param progressValue the best value reached towards the threshold
 * @param progressRatio the ratio of the progress towards the threshold
 * @param currentValue the current record of the scoped exercise
 * @param unlocked whether the achievement has been unlocked
 * @param unlockedAt the instant the achievement was unlocked
 * @param seen whether the user has seen the unlock
 */
@Schema(description = "Achievement of the catalog with the state of the user")
public record AchievementResponse(
        @Schema(description = "Achievement code", example = "WORKOUTS_10")
        String code,

        @Schema(description = "Achievement name", example = "Ten workouts")
        String name,

        @Schema(description = "What the user has to do to earn the achievement")
        String description,

        @Schema(description = "Category of the achievement", example = "MILESTONE", nullable = true)
        AchievementCategory category,

        @Schema(description = "Rarity of the achievement", example = "BRONZE", nullable = true)
        AchievementTier tier,

        @Schema(description = "Kind of rule that unlocks the achievement", example = "THRESHOLD", nullable = true)
        RuleType ruleType,

        @Schema(description = "Metric the achievement is measured against", example = "TOTAL_WORKOUTS", nullable = true)
        AchievementMetric metric,

        @Schema(description = "Value the metric has to reach", example = "10", nullable = true)
        Double threshold,

        @Schema(description = "How many badges the rule produces", example = "GLOBAL", nullable = true)
        AchievementScope scope,

        @Schema(description = "Key of the image that represents the achievement", nullable = true)
        String iconKey,

        @Schema(description = "Identifier of the state of the user", nullable = true)
        UUID userAchievementId,

        @Schema(description = "Exercise the achievement is scoped to", nullable = true)
        UUID exerciseId,

        @Schema(
                description = "Name of the exercise the achievement is scoped to",
                example = "Bench press",
                nullable = true)
        String exerciseName,

        @Schema(description = "Best value reached towards the threshold", example = "7", nullable = true)
        Double progressValue,

        @Schema(description = "Ratio of the progress towards the threshold", example = "0.7", nullable = true)
        Double progressRatio,

        @Schema(description = "Current record of the scoped exercise", example = "92.5", nullable = true)
        Double currentValue,

        @Schema(description = "Whether the achievement has been unlocked")
        boolean unlocked,

        @Schema(description = "Instant the achievement was unlocked", nullable = true)
        Instant unlockedAt,

        @Schema(description = "Whether the user has seen the unlock")
        boolean seen) {

    public AchievementResponse {
        progressRatio = threshold == null || threshold <= 0 || progressValue == null
                ? null
                : Math.min(1.0, progressValue / threshold);
    }
}
