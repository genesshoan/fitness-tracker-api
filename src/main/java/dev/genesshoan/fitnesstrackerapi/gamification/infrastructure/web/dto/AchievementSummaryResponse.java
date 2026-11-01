package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto;

import java.util.List;
import java.util.Map;

import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementCategory;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The state of the collection of a user.
 *
 * @param totalAvailable the number of achievements in the catalog
 * @param totalUnlocked the number of achievements the user unlocked
 * @param unseenCount the number of unlocks the user has not seen yet
 * @param byCategory the unlocked and total counts of every category
 * @param next the achievements the user is closest to unlocking
 */
@Schema(description = "State of the collection of the user")
public record AchievementSummaryResponse(
        @Schema(description = "Number of achievements in the catalog", example = "29")
        long totalAvailable,

        @Schema(description = "Number of achievements the user unlocked", example = "12")
        long totalUnlocked,

        @Schema(description = "Number of unlocks the user has not seen yet", example = "2")
        long unseenCount,

        @Schema(description = "Unlocked and total counts of every category")
        Map<AchievementCategory, AchievementCategorySummary> byCategory,

        @Schema(description = "Achievements the user is closest to unlocking")
        List<NextAchievement> next) {}
