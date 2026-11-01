package dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result;

import java.util.List;
import java.util.Map;

import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementCategory;

/**
 * The state of the collection of a user.
 *
 * @param totalAvailable the number of achievements in the catalog
 * @param totalUnlocked the number of achievements the user unlocked
 * @param unseenCount the number of unlocks the user has not seen yet
 * @param byCategory the unlocked and total counts of every category
 * @param next the achievements the user is closest to unlocking
 */
public record AchievementSummaryResult(
        long totalAvailable,
        long totalUnlocked,
        long unseenCount,
        Map<AchievementCategory, AchievementCategorySummary> byCategory,
        List<NextAchievement> next) {

    /**
     * The counts of one category.
     *
     * @param unlocked the number of achievements the user unlocked
     * @param total the number of achievements in the category
     */
    public record AchievementCategorySummary(long unlocked, long total) {}

    /**
     * An achievement the user has not unlocked yet.
     *
     * @param code the stable key of the achievement
     * @param name the display name of the achievement
     * @param progressValue the best value reached towards the threshold
     * @param threshold the value the metric has to reach
     * @param remaining how much is missing to reach the threshold
     */
    public record NextAchievement(String code, String name, Double progressValue, Double threshold, Double remaining) {}
}
