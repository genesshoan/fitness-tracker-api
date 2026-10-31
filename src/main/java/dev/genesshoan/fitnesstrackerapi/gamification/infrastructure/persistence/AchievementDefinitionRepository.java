package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementDefinition;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.RuleType;

/**
 * Persistence operations for the achievement catalog.
 */
@Repository
public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, UUID> {

    /**
     * Finds an achievement by its stable key.
     *
     * @param code the key identifying the achievement
     * @return the matching achievement, or {@link Optional#empty()} if no
     *         achievement with the given key exists
     */
    Optional<AchievementDefinition> findByCode(String code);

    /**
     * Finds the achievements that are currently offered, in catalog order.
     *
     * @return the active achievements, ordered by their position in the catalog
     */
    List<AchievementDefinition> findByActiveTrueOrderBySortOrderAsc();

    /**
     * Finds the active achievements that use the specified kind of rule.
     *
     * @param ruleType the kind of rule to look for
     * @return the matching active achievements
     */
    List<AchievementDefinition> findByRuleTypeAndActiveTrue(RuleType ruleType);
}
