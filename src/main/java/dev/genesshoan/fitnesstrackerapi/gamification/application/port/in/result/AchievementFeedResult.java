package dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result;

import java.time.Instant;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementCategory;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementTier;

/**
 * An achievement that the user unlocked, as shown in the feed.
 *
 * @param id the identifier of the state
 * @param code the stable key of the achievement
 * @param name the display name of the achievement
 * @param tier the visual rarity of the achievement
 * @param category the subject the achievement belongs to
 * @param exerciseId the exercise the achievement is scoped to, or {@code null}
 *        for a global achievement
 * @param exerciseName the name of the exercise the achievement is scoped to
 * @param unlockedAt the instant the achievement was unlocked
 * @param seen whether the user has seen the unlock
 */
public record AchievementFeedResult(
        UUID id,
        String code,
        String name,
        AchievementTier tier,
        AchievementCategory category,
        UUID exerciseId,
        String exerciseName,
        Instant unlockedAt,
        boolean seen) {}
