package dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result;

import java.time.Instant;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementTier;

/**
 * An achievement that was unlocked by a training event.
 *
 * @param id the identifier of the user achievement
 * @param code the stable key of the achievement
 * @param name the display name of the achievement
 * @param tier the visual rarity of the achievement
 * @param exerciseId the exercise the achievement is scoped to, or {@code null}
 *        for a global achievement
 * @param unlockedAt the instant the achievement was unlocked
 */
public record UnlockedAchievementResult(
        UUID id, String code, String name, AchievementTier tier, UUID exerciseId, Instant unlockedAt) {}
