package dev.genesshoan.fitnesstrackerapi.gamification.application.port.in;

import java.util.List;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.AchievementFeedResult;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.AchievementSummaryResult;

/**
 * Reads the feed of unlocked achievements and the state of the collection.
 */
public interface AchievementFeedUseCase {

    /**
     * Returns the achievements the user unlocked, most recent first.
     *
     * @param userId the user whose achievements are looked for
     * @param unseenOnly whether to keep only the unlocks the user has not seen
     *        yet
     * @param limit the maximum number of achievements to return
     * @return the unlocked achievements, most recent first
     */
    List<AchievementFeedResult> getFeed(UUID userId, boolean unseenOnly, int limit);

    /**
     * Marks the specified achievements of the user as seen.
     *
     * @param userId the user whose achievements are marked
     * @param achievementIds the identifiers of the achievements to mark
     */
    void markSeen(UUID userId, List<UUID> achievementIds);

    /**
     * Returns the state of the collection of the user.
     *
     * @param userId the user the summary belongs to
     * @return the state of the collection
     */
    AchievementSummaryResult getSummary(UUID userId);
}
