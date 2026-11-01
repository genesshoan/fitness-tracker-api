package dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result;

import java.time.Instant;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementCategory;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementMetric;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementScope;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementTier;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.RuleType;

/**
 * One achievement of the catalog, together with the state the user reached.
 *
 * <p>A global achievement produces one entry. An achievement scoped to an
 * exercise produces one entry for each exercise the user earned it on.</p>
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
 * @param userAchievementId the identifier of the state, or {@code null} when the
 *        user has no state for it yet
 * @param exerciseId the exercise the achievement is scoped to, or {@code null}
 *        for a global achievement
 * @param exerciseName the name of the exercise the achievement is scoped to
 * @param progressValue the best value reached towards the threshold
 * @param currentValue the current record of the scoped exercise
 * @param unlocked whether the achievement has been unlocked
 * @param unlockedAt the instant the achievement was unlocked
 * @param seen whether the user has seen the unlock
 */
public record AchievementCatalogResult(
        String code,
        String name,
        String description,
        AchievementCategory category,
        AchievementTier tier,
        RuleType ruleType,
        AchievementMetric metric,
        Double threshold,
        AchievementScope scope,
        String iconKey,
        UUID userAchievementId,
        UUID exerciseId,
        String exerciseName,
        Double progressValue,
        Double currentValue,
        boolean unlocked,
        Instant unlockedAt,
        boolean seen) {}
