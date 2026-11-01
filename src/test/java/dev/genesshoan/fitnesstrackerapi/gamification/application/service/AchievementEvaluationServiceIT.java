package dev.genesshoan.fitnesstrackerapi.gamification.application.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.UnlockedAchievementResult;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.PersonalRecordMetric;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.PersonalRecordRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.UserAchievementRepository;
import dev.genesshoan.fitnesstrackerapi.testdata.TestEntityFactory;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionSetBuilder;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the whole evaluation against the database, the catalog, and the
 * training history.
 *
 * <p>The test runs in a transaction that is rolled back, so the achievements it
 * unlocks do not leak into the other tests that share the container.</p>
 */
@Transactional
class AchievementEvaluationServiceIT extends AbstractIntegrationTest {

    @Autowired
    private AchievementEvaluationService achievementEvaluationService;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private PersonalRecordRepository personalRecordRepository;

    @Autowired
    private TestEntityFactory testEntityFactory;

    @Test
    @DisplayName("Should unlock the milestone and the record achievements of a completed session")
    void onSessionCompleted_ShouldUnlockTheMilestoneAndTheRecordAchievements() {
        // Given
        User user = testEntityFactory.createAndPersistUser();
        var exercise = testEntityFactory.createAndPersistExercise();
        WorkoutSession session = testEntityFactory.createAndPersistCompletedWorkoutSession(user);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(1)
                .withReps(10)
                .withWeightKg(100.0)
                .withCompleted(true));

        // When
        List<UnlockedAchievementResult> unlocked =
                achievementEvaluationService.onSessionCompleted(user.getId(), session.getId());

        // Then
        assertThat(unlocked)
                .extracting(UnlockedAchievementResult::code)
                .contains("FIRST_WORKOUT", "PR_FIRST_MAX_WEIGHT", "PR_MAX_WEIGHT_PER_EXERCISE");

        var record = personalRecordRepository.findByUserIdAndExerciseIdAndMetricAndWeight(
                user.getId(), exercise.getId(), PersonalRecordMetric.MAX_WEIGHT, null);
        assertThat(record).isPresent();
        assertThat(record.get().getValue()).isEqualTo(100.0);

        assertThat(userAchievementRepository.findByUserIdAndUnlockedTrueOrderByUnlockedAtDesc(
                        user.getId(), org.springframework.data.domain.PageRequest.of(0, 50)))
                .extracting(achievement -> achievement.getDefinition().getCode())
                .contains("FIRST_WORKOUT", "PR_FIRST_MAX_WEIGHT", "PR_MAX_WEIGHT_PER_EXERCISE");
    }

    @Test
    @DisplayName("Should not unlock the same achievement twice")
    void onSessionCompleted_ShouldNotUnlockTheSameAchievementTwice() {
        // Given
        User user = testEntityFactory.createAndPersistUser();
        var exercise = testEntityFactory.createAndPersistExercise();
        WorkoutSession session = testEntityFactory.createAndPersistCompletedWorkoutSession(user);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(1)
                .withReps(10)
                .withWeightKg(100.0)
                .withCompleted(true));

        achievementEvaluationService.onSessionCompleted(user.getId(), session.getId());

        // When
        List<UnlockedAchievementResult> unlocked =
                achievementEvaluationService.onSessionCompleted(user.getId(), session.getId());

        // Then
        assertThat(unlocked).isEmpty();
    }

    @Test
    @DisplayName("Should keep the record when a later set does not beat it")
    void onSessionCompleted_ShouldKeepTheBestRecord() {
        // Given
        User user = testEntityFactory.createAndPersistUser();
        var exercise = testEntityFactory.createAndPersistExercise();
        WorkoutSession session = testEntityFactory.createAndPersistCompletedWorkoutSession(user);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(1)
                .withReps(10)
                .withWeightKg(120.0)
                .withCompleted(true));
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(2)
                .withReps(10)
                .withWeightKg(100.0)
                .withCompleted(true));

        // When
        achievementEvaluationService.onSessionCompleted(user.getId(), session.getId());

        // Then
        var record = personalRecordRepository.findByUserIdAndExerciseIdAndMetricAndWeight(
                user.getId(), exercise.getId(), PersonalRecordMetric.MAX_WEIGHT, null);
        assertThat(record).isPresent();
        assertThat(record.get().getValue()).isEqualTo(120.0);
    }

    @Test
    @DisplayName("Should rebuild the records of an exercise when they are recalculated")
    void refreshExerciseRecords_ShouldRebuildTheRecordsFromTheHistory() {
        // Given
        User user = testEntityFactory.createAndPersistUser();
        var exercise = testEntityFactory.createAndPersistExercise();
        WorkoutSession session = testEntityFactory.createAndPersistCompletedWorkoutSession(user);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(1)
                .withReps(10)
                .withWeightKg(120.0)
                .withCompleted(true));

        achievementEvaluationService.onSessionCompleted(user.getId(), session.getId());

        // When
        achievementEvaluationService.refreshExerciseRecords(user.getId(), exercise.getId());

        // Then
        var record = personalRecordRepository.findByUserIdAndExerciseIdAndMetricAndWeight(
                user.getId(), exercise.getId(), PersonalRecordMetric.MAX_WEIGHT, null);
        assertThat(record).isPresent();
        assertThat(record.get().getValue()).isEqualTo(120.0);
        assertThat(record.get().getAchievedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should unlock a record achievement when a single set is completed")
    void onSetCompleted_ShouldUnlockTheRecordAchievement() {
        // Given
        User user = testEntityFactory.createAndPersistUser();
        var exercise = testEntityFactory.createAndPersistExercise();
        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(user);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        var set = testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(1)
                .withReps(10)
                .withWeightKg(100.0)
                .withCompleted(true));

        // When
        List<UnlockedAchievementResult> unlocked =
                achievementEvaluationService.onSetCompleted(user.getId(), session.getId(), set.getId());

        // Then
        assertThat(unlocked)
                .extracting(UnlockedAchievementResult::code)
                .contains("PR_FIRST_MAX_WEIGHT", "PR_MAX_WEIGHT_PER_EXERCISE");

        var record = personalRecordRepository.findByUserIdAndExerciseIdAndMetricAndWeight(
                user.getId(), exercise.getId(), PersonalRecordMetric.MAX_WEIGHT, null);
        assertThat(record).isPresent();
        assertThat(record.get().getValue()).isEqualTo(100.0);
        assertThat(record.get().getAchievedAt()).isNotNull();
        assertThat(record.get().getAchievedAt()).isBeforeOrEqualTo(Instant.now());
    }
}
