package dev.genesshoan.fitnesstrackerapi.repository;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.stats.repository.StatsRepository;
import dev.genesshoan.fitnesstrackerapi.testdata.TestEntityFactory;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionSetBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.WorkoutSessionBuilder;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionStatus;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatsRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private StatsRepository statsRepository;

    @Autowired
    private TestEntityFactory testEntityFactory;

    @Test
    @DisplayName("Should find ranked completed sets before a session")
    void findRankedSets_shouldGroupTopSetsByExercise() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise =
                testEntityFactory.createAndPersistExercise(ExerciseBuilder.anExercise(testEntityFactory.faker()));
        Instant startedAt = Instant.parse("2026-09-13T10:00:00Z");
        WorkoutSession session = persistCompletedSession(user, startedAt, startedAt.plusSeconds(3600));
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withWeightKg(100.0)
                .withReps(5)
                .withCompleted(true));

        var result = statsRepository.findRankedSets(user.getId(), Set.of(exercise.getId()), startedAt.plusSeconds(1));

        assertThat(result).containsKey(exercise.getId());
        assertThat(result.get(exercise.getId())).anySatisfy(set -> {
            assertThat(set.weightKg()).isEqualTo(100.0);
            assertThat(set.reps()).isEqualTo(5);
            assertThat(set.rnWeight()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("Should return no ranked sets when no exercises are requested")
    void findRankedSets_shouldReturnEmptyForNoExerciseIds() {
        assertThat(statsRepository.findRankedSets(UUID.randomUUID(), Set.of(), Instant.now()))
                .isEmpty();
    }

    @Test
    @DisplayName("Should not return ranked sets for another user")
    void findRankedSets_shouldFilterByUser() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        Instant startedAt = Instant.parse("2026-09-13T10:00:00Z");
        WorkoutSession session = persistCompletedSession(owner, startedAt, startedAt.plusSeconds(3600));
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withCompleted(true));

        assertThat(statsRepository.findRankedSets(
                        otherUser.getId(), Set.of(exercise.getId()), startedAt.plusSeconds(1)))
                .isEmpty();
    }

    @Test
    @DisplayName("Should return trained dates in ascending order")
    void getTrainedDatesBeforeAsc_shouldReturnOrderedDistinctDates() {
        User user = testEntityFactory.createAndPersistUser();
        Instant first = Instant.parse("2026-09-10T10:00:00Z");
        persistCompletedSession(user, first, first);
        persistCompletedSession(user, first, first);
        Instant second = Instant.parse("2026-09-12T10:00:00Z");
        persistCompletedSession(user, second, second);

        assertThat(statsRepository.getTrainedDatesBeforeAsc(user.getId(), Instant.parse("2026-09-13T00:00:00Z")))
                .containsExactly(first, second);
    }

    @Test
    @DisplayName("Should exclude trained dates at or after the reference instant")
    void getTrainedDatesBeforeAsc_shouldRespectReferenceInstant() {
        User user = testEntityFactory.createAndPersistUser();
        Instant included = Instant.parse("2026-09-10T10:00:00Z");
        Instant excluded = Instant.parse("2026-09-12T10:00:00Z");
        persistCompletedSession(user, included, included);
        persistCompletedSession(user, excluded, excluded);

        assertThat(statsRepository.getTrainedDatesBeforeAsc(user.getId(), excluded))
                .containsExactly(included);
    }

    @Test
    @DisplayName("Should return no trained dates for another user")
    void getTrainedDatesBeforeAsc_shouldFilterByUser() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        Instant completedAt = Instant.parse("2026-09-10T10:00:00Z");
        persistCompletedSession(owner, completedAt, completedAt);

        assertThat(statsRepository.getTrainedDatesBeforeAsc(otherUser.getId(), completedAt.plusSeconds(1)))
                .isEmpty();
    }

    @Test
    @DisplayName("Should return only completed weighted sets for session volume")
    void getSetsForVolume_shouldReturnCompletedWeightedSets() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        WorkoutSession session = persistCompletedSession(user, Instant.now().minusSeconds(100), Instant.now());
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withWeightKg(50.0)
                .withReps(10)
                .withCompleted(true));
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(2)
                .withWeightKg(null)
                .withCompleted(true));

        assertThat(statsRepository.getSetsForVolume(session.getId(), user.getId()))
                .containsExactly(
                        new dev.genesshoan.fitnesstrackerapi.stats.repository.projection.VolumeSetProjection(50.0, 10));
    }

    @Test
    @DisplayName("Should return no volume sets for another user")
    void getSetsForVolume_shouldFilterByUser() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        WorkoutSession session = persistCompletedSession(owner, Instant.now().minusSeconds(100), Instant.now());
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withCompleted(true));

        assertThat(statsRepository.getSetsForVolume(session.getId(), otherUser.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("Should return the highest estimated one rep max")
    void getSetForOneRepMax_shouldReturnHighestValue() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        WorkoutSession session = persistCompletedSession(user, Instant.now().minusSeconds(100), Instant.now());
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withWeightKg(80.0)
                .withReps(5)
                .withCompleted(true));
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(2)
                .withWeightKg(100.0)
                .withReps(5)
                .withCompleted(true));

        assertThat(statsRepository.getSetForOneRepMax(user.getId(), exercise.getId()))
                .hasValueSatisfying(
                        result -> assertThat(result.estimatedOneRepMax()).isEqualTo(116.66666666666667));
    }

    @Test
    @DisplayName("Should return empty one rep max when no qualifying sets exist")
    void getSetForOneRepMax_shouldReturnEmptyWhenNoSetsExist() {
        assertThat(statsRepository.getSetForOneRepMax(UUID.randomUUID(), UUID.randomUUID()))
                .isEmpty();
    }

    @Test
    @DisplayName("Should return the strongest set from each session in progress range")
    void getExerciseProgressPoints_shouldReturnBestSetPerSession() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        Instant completedAt = Instant.parse("2026-09-10T10:00:00Z");
        WorkoutSession session = persistCompletedSession(user, completedAt.minusSeconds(3600), completedAt);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withWeightKg(80.0)
                .withReps(5)
                .withCompleted(true));
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(2)
                .withWeightKg(100.0)
                .withReps(5)
                .withCompleted(true));

        var result = statsRepository.getExerciseProgressPoints(
                user.getId(), exercise.getId(), completedAt.minusSeconds(1), completedAt.plusSeconds(1));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().weightKg()).isEqualTo(100.0);
        assertThat(result.getFirst().reps()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should return no progress points outside the requested range")
    void getExerciseProgressPoints_shouldRespectDateRange() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        Instant completedAt = Instant.parse("2026-09-10T10:00:00Z");
        WorkoutSession session = persistCompletedSession(user, completedAt.minusSeconds(3600), completedAt);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withCompleted(true));

        assertThat(statsRepository.getExerciseProgressPoints(
                        user.getId(), exercise.getId(), completedAt.plusSeconds(1), completedAt.plusSeconds(2)))
                .isEmpty();
    }

    private WorkoutSession persistCompletedSession(User user, Instant startedAt, Instant completedAt) {
        return testEntityFactory.createAndPersistWorkoutSession(
                WorkoutSessionBuilder.aWorkoutSession(testEntityFactory.faker())
                        .forUser(user)
                        .withStatus(SessionStatus.COMPLETED)
                        .withStartedAt(startedAt)
                        .withCompletedAt(completedAt));
    }
}
