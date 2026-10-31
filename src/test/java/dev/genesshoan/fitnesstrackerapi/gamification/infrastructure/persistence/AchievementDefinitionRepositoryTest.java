package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import dev.genesshoan.fitnesstrackerapi.base.AbstractPostgresTest;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementDefinition;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.RuleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AchievementDefinitionRepositoryTest extends AbstractPostgresTest {

    @Autowired
    AchievementDefinitionRepository achievementDefinitionRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    @DisplayName("Should return the seeded achievements in catalog order")
    void findByActiveTrueOrderBySortOrderAsc_ShouldReturnTheSeededCatalog() {
        // Given
        // The catalog is seeded by the migration that creates the table

        // When
        var result = achievementDefinitionRepository.findByActiveTrueOrderBySortOrderAsc();

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(AchievementDefinition::isActive);
        assertThat(result)
                .extracting(AchievementDefinition::getSortOrder)
                .isSorted()
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Should find a seeded achievement by its code")
    void findByCode_ShouldReturnTheSeededAchievement() {
        // Given
        var code = "WORKOUTS_10";

        // When
        var result = achievementDefinitionRepository.findByCode(code);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getThreshold()).isEqualTo(10.0);
        assertThat(result.get().getRuleType()).isEqualTo(RuleType.THRESHOLD);
        assertThat(result.get().getScope().name()).isEqualTo("GLOBAL");
    }

    @Test
    @DisplayName("Should split the seeded catalog by rule type")
    void findByRuleTypeAndActiveTrue_ShouldReturnTheMatchingRules() {
        // Given
        // The catalog is seeded by the migration that creates the table

        // When
        var thresholds = achievementDefinitionRepository.findByRuleTypeAndActiveTrue(RuleType.THRESHOLD);
        var records = achievementDefinitionRepository.findByRuleTypeAndActiveTrue(RuleType.PERSONAL_RECORD);

        // Then
        assertThat(thresholds).isNotEmpty().allMatch(d -> d.getThreshold() != null);
        assertThat(records).isNotEmpty().allMatch(d -> d.getThreshold() == null);
        assertThat(thresholds).allMatch(d -> d.getMetric().isThresholdMetric());
    }

    @Test
    @DisplayName("Should accept every seeded achievement as a coherent rule")
    void validate_ShouldAcceptEverySeededAchievement() {
        // Given
        var catalog = achievementDefinitionRepository.findByActiveTrueOrderBySortOrderAsc();

        // When
        var failures = new ArrayList<String>();
        for (var definition : catalog) {
            try {
                definition.validate();
            } catch (IllegalArgumentException e) {
                failures.add(definition.getCode() + ": " + e.getMessage());
            }
        }

        // Then
        assertThat(failures).isEmpty();
    }
}
