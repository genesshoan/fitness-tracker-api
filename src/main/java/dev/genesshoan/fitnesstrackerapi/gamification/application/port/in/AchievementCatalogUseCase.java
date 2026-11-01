package dev.genesshoan.fitnesstrackerapi.gamification.application.port.in;

import java.util.List;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.AchievementCatalogResult;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementCategory;

/**
 * Reads the catalog of achievements and the state the user reached in it.
 */
public interface AchievementCatalogUseCase {

    /**
     * Returns the catalog of achievements with the state of the user.
     *
     * @param userId the user the catalog belongs to
     * @param category the category to filter by, or {@code null} for every
     *        category
     * @param unlockedOnly whether to keep only the achievements the user
     *        unlocked
     * @param unseenOnly whether to keep only the unlocks the user has not seen
     *        yet
     * @return the catalog with the state of the user
     */
    List<AchievementCatalogResult> getCatalog(
            UUID userId, AchievementCategory category, boolean unlockedOnly, boolean unseenOnly);

    /**
     * Returns the entries of one achievement of the catalog with the state of
     * the user.
     *
     * <p>A global achievement produces one entry. An achievement scoped to an
     * exercise produces one entry for each exercise the user earned it on.</p>
     *
     * @param userId the user the achievement belongs to
     * @param code the stable key of the achievement
     * @return the entries of the achievement
     * @throws dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException
     *         if no active achievement exists with that key
     */
    List<AchievementCatalogResult> getByCode(UUID userId, String code);
}
