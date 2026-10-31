package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import dev.genesshoan.fitnesstrackerapi.base.AbstractPostgresTest;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementCategory;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementDefinition;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementMetric;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementScope;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementTier;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.ComparisonOperator;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.RuleType;
import dev.genesshoan.fitnesstrackerapi.testdata.TestEntityFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserAchievementRepositoryTest extends AbstractPostgresTest {

    @Autowired
    UserAchievementRepository userAchievementRepository;

    @Autowired
    AchievementDefinitionRepository achievementDefinitionRepository;

    @Autowired
    TestEntityFactory testEntityFactory;

    @Autowired
    TestEntityManager entityManager;

    @Test
    @DisplayName("Should create the state of an achievement on the first progress")
    void upsert_ShouldCreateTheRowOnTheFirstProgress() {
        // Given
        var user = testEntityFactory.createAndPersistUser();
        var definition = createDefinition("TEST_FIRST_PROGRESS");

        // When
        userAchievementRepository.upsert(
                UUID.randomUUID(), user.getId(), definition.getId(), null, 7.0, false, null, null, null);
        entityManager.flush();
        entityManager.clear();

        // Then
        var result =
                userAchievementRepository.findByUserIdAndDefinitionAndExercise(user.getId(), definition.getId(), null);
        assertThat(result).isPresent();
        assertThat(result.get().getProgressValue()).isEqualTo(7.0);
        assertThat(result.get().isUnlocked()).isFalse();
    }

    @Test
    @DisplayName("Should keep the best progress and never move it backwards")
    void upsert_ShouldKeepTheBestProgress() {
        // Given
        var user = testEntityFactory.createAndPersistUser();
        var definition = createDefinition("TEST_BEST_PROGRESS");
        userAchievementRepository.upsert(
                UUID.randomUUID(), user.getId(), definition.getId(), null, 12.0, false, null, null, null);

        // When
        userAchievementRepository.upsert(
                UUID.randomUUID(), user.getId(), definition.getId(), null, 3.0, false, null, null, null);
        entityManager.flush();
        entityManager.clear();

        // Then
        var result =
                userAchievementRepository.findByUserIdAndDefinitionAndExercise(user.getId(), definition.getId(), null);
        assertThat(result).isPresent();
        assertThat(result.get().getProgressValue()).isEqualTo(12.0);
        assertThat(userAchievementRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Should keep the instant of the first unlock")
    void upsert_ShouldKeepTheFirstUnlockInstant() {
        // Given
        var user = testEntityFactory.createAndPersistUser();
        var definition = createDefinition("TEST_UNLOCK_INSTANT");
        var firstUnlock = Instant.parse("2026-10-01T10:00:00Z");
        userAchievementRepository.upsert(
                UUID.randomUUID(), user.getId(), definition.getId(), null, 10.0, true, firstUnlock, null, null);

        // When
        userAchievementRepository.upsert(
                UUID.randomUUID(),
                user.getId(),
                definition.getId(),
                null,
                20.0,
                true,
                Instant.parse("2026-10-20T10:00:00Z"),
                null,
                null);
        entityManager.flush();
        entityManager.clear();

        // Then
        var result =
                userAchievementRepository.findByUserIdAndDefinitionAndExercise(user.getId(), definition.getId(), null);
        assertThat(result).isPresent();
        assertThat(result.get().isUnlocked()).isTrue();
        assertThat(result.get().getUnlockedAt()).isEqualTo(firstUnlock);
    }

    @Test
    @DisplayName("Should keep one row per exercise for an achievement scoped to an exercise")
    void upsert_ShouldKeepOneRowPerExercise() {
        // Given
        var user = testEntityFactory.createAndPersistUser();
        var definition = createDefinition("TEST_SCOPED");
        var firstExercise = testEntityFactory.createAndPersistExercise();
        var secondExercise = testEntityFactory.createAndPersistExercise();

        // When
        userAchievementRepository.upsert(
                UUID.randomUUID(),
                user.getId(),
                definition.getId(),
                firstExercise.getId(),
                5.0,
                true,
                Instant.now(),
                null,
                null);
        userAchievementRepository.upsert(
                UUID.randomUUID(),
                user.getId(),
                definition.getId(),
                secondExercise.getId(),
                5.0,
                true,
                Instant.now(),
                null,
                null);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(userAchievementRepository.findAll()).hasSize(2);
        assertThat(userAchievementRepository.findByUserIdAndDefinitionAndExercise(
                        user.getId(), definition.getId(), firstExercise.getId()))
                .isPresent();
        assertThat(userAchievementRepository.findByUserIdAndDefinitionAndExercise(
                        user.getId(), definition.getId(), secondExercise.getId()))
                .isPresent();
    }

    @Test
    @DisplayName("Should return the unlocked achievements of a user, most recent first")
    void findByUserIdAndUnlockedTrueOrderByUnlockedAtDesc_ShouldReturnTheFeed() {
        // Given
        var user = testEntityFactory.createAndPersistUser();
        var olderDefinition = createDefinition("TEST_FEED_OLDER");
        var newerDefinition = createDefinition("TEST_FEED_NEWER");
        var older = Instant.parse("2026-10-01T10:00:00Z");
        var newer = Instant.parse("2026-10-10T10:00:00Z");
        userAchievementRepository.upsert(
                UUID.randomUUID(), user.getId(), olderDefinition.getId(), null, 10.0, true, older, null, null);
        userAchievementRepository.upsert(
                UUID.randomUUID(), user.getId(), newerDefinition.getId(), null, 20.0, true, newer, null, null);
        entityManager.flush();
        entityManager.clear();

        // When
        var result = userAchievementRepository.findByUserIdAndUnlockedTrueOrderByUnlockedAtDesc(
                user.getId(), PageRequest.of(0, 10));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUnlockedAt()).isEqualTo(newer);
        assertThat(result.get(1).getUnlockedAt()).isEqualTo(older);
        assertThat(result.get(0).getDefinition().getCode()).isEqualTo("TEST_FEED_NEWER");
    }

    @Test
    @DisplayName("Should mark only the given achievements of a user as seen")
    void markSeen_ShouldMarkOnlyTheGivenAchievements() {
        // Given
        var user = testEntityFactory.createAndPersistUser();
        var definition = createDefinition("TEST_SEEN");
        userAchievementRepository.upsert(
                UUID.randomUUID(), user.getId(), definition.getId(), null, 10.0, true, Instant.now(), null, null);
        entityManager.flush();
        entityManager.clear();
        var achievement = userAchievementRepository
                .findByUserIdAndDefinitionAndExercise(user.getId(), definition.getId(), null)
                .orElseThrow();

        // When
        var affected = userAchievementRepository.markSeen(user.getId(), java.util.List.of(achievement.getId()));
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(affected).isEqualTo(1);
        assertThat(userAchievementRepository.findByUserIdAndUnlockedTrueAndSeenFalseOrderByUnlockedAtDesc(user.getId()))
                .isEmpty();
    }

    private AchievementDefinition createDefinition(String code) {
        return achievementDefinitionRepository.save(AchievementDefinition.builder()
                .code(code)
                .name("Test achievement")
                .description("Achievement used by the repository tests")
                .category(AchievementCategory.MILESTONE)
                .tier(AchievementTier.BRONZE)
                .ruleType(RuleType.THRESHOLD)
                .metric(AchievementMetric.TOTAL_WORKOUTS)
                .operator(ComparisonOperator.GTE)
                .threshold(10.0)
                .scope(AchievementScope.GLOBAL)
                .sortOrder(99)
                .build());
    }
}
