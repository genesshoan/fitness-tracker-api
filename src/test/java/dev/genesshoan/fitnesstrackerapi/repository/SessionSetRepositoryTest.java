package dev.genesshoan.fitnesstrackerapi.repository;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.base.AbstractPostgresTest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.testdata.TestEntityFactory;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.RoutineBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionSetBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.WorkoutSessionBuilder;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import dev.genesshoan.fitnesstrackerapi.workout.repository.SessionSetRepository;
import dev.genesshoan.fitnesstrackerapi.workout.repository.WorkoutSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SessionSetRepositoryTest extends AbstractPostgresTest {

    @Autowired
    SessionSetRepository sessionSetRepository;

    @Autowired
    WorkoutSessionRepository workoutSessionRepository;

    @Autowired
    TestEntityFactory testEntityFactory;

    @Test
    @DisplayName("Should load workout session, exercise and sets with session set")
    void findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise_ShouldLoadAssociations() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise =
                testEntityFactory.createAndPersistExercise(ExerciseBuilder.anExercise(testEntityFactory.faker()));
        Routine routine = testEntityFactory.createAndPersistRoutine(
                RoutineBuilder.aRoutine(testEntityFactory.faker()).forUser(user));
        WorkoutSession session = workoutSessionRepository.saveAndFlush(
                dev.genesshoan.fitnesstrackerapi.testdata.builder.WorkoutSessionBuilder.aWorkoutSession(
                                testEntityFactory.faker())
                        .forUser(user)
                        .withRoutine(routine)
                        .build());

        SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(testEntityFactory.faker())
                .forWorkoutSession(session)
                .forExercise(exercise)
                .build();
        session.addExerciseAt(sessionExercise, 1);
        workoutSessionRepository.saveAndFlush(session);

        SessionSet sessionSet = SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(1)
                .build();
        sessionExercise.addSet(sessionSet);
        sessionSetRepository.saveAndFlush(sessionSet);

        var result = sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                sessionSet.getId(), sessionExercise.getId(), session.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getSessionExercise().getId()).isEqualTo(sessionExercise.getId());
        assertThat(result.get().getSessionExercise().getWorkoutSession().getId())
                .isEqualTo(session.getId());
        assertThat(result.get()
                        .getSessionExercise()
                        .getWorkoutSession()
                        .getUser()
                        .getId())
                .isEqualTo(user.getId());

        assertThat(result.get().getSessionExercise().getExercise().getId()).isEqualTo(exercise.getId());
    }

    @Test
    @DisplayName("Should return empty when workout session belongs to another user")
    void findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise_ShouldReturnEmptyWhenUserDiffers() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        Exercise exercise =
                testEntityFactory.createAndPersistExercise(ExerciseBuilder.anExercise(testEntityFactory.faker()));
        Routine routine = testEntityFactory.createAndPersistRoutine(
                RoutineBuilder.aRoutine(testEntityFactory.faker()).forUser(owner));
        WorkoutSession session = workoutSessionRepository.saveAndFlush(
                dev.genesshoan.fitnesstrackerapi.testdata.builder.WorkoutSessionBuilder.aWorkoutSession(
                                testEntityFactory.faker())
                        .forUser(owner)
                        .withRoutine(routine)
                        .build());

        SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(testEntityFactory.faker())
                .forWorkoutSession(session)
                .forExercise(exercise)
                .build();
        session.addExerciseAt(sessionExercise, 1);
        workoutSessionRepository.saveAndFlush(session);

        SessionSet sessionSet = SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(1)
                .build();
        sessionExercise.addSet(sessionSet);
        sessionSetRepository.saveAndFlush(sessionSet);

        var result = sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                sessionSet.getId(), sessionExercise.getId(), session.getId(), otherUser.getId());

        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should return empty when ids do not exist")
    void findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise_ShouldReturnEmptyWhenIdsAreMissing() {
        var result = sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should return max set number for session exercise")
    void findMaxSetNumberBySessionExerciseId_ShouldReturnMaxSetNumber() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise =
                testEntityFactory.createAndPersistExercise(ExerciseBuilder.anExercise(testEntityFactory.faker()));
        Routine routine = testEntityFactory.createAndPersistRoutine(
                RoutineBuilder.aRoutine(testEntityFactory.faker()).forUser(user));
        WorkoutSession session =
                workoutSessionRepository.saveAndFlush(WorkoutSessionBuilder.aWorkoutSession(testEntityFactory.faker())
                        .forUser(user)
                        .withRoutine(routine)
                        .build());

        SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(testEntityFactory.faker())
                .forWorkoutSession(session)
                .forExercise(exercise)
                .build();
        session.addExerciseAt(sessionExercise, 1);
        workoutSessionRepository.saveAndFlush(session);

        SessionSet firstSet = SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(1)
                .build();
        SessionSet secondSet = SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(2)
                .build();
        sessionExercise.addSet(firstSet);
        sessionExercise.addSet(secondSet);
        sessionSetRepository.saveAndFlush(firstSet);
        sessionSetRepository.saveAndFlush(secondSet);

        int maxSetNumber = sessionSetRepository.findMaxSetNumberBySessionExerciseId(sessionExercise.getId());

        assertThat(maxSetNumber).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return last set metrics for exercise from completed sessions")
    void findLastSetsByExerciseIdAndUserId_ShouldReturnLastSetMetrics() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise =
                testEntityFactory.createAndPersistExercise(ExerciseBuilder.anExercise(testEntityFactory.faker()));

        WorkoutSession completedSession =
                workoutSessionRepository.saveAndFlush(WorkoutSessionBuilder.aWorkoutSession(testEntityFactory.faker())
                        .forUser(user)
                        .withStatus(dev.genesshoan.fitnesstrackerapi.workout.domain.SessionStatus.COMPLETED)
                        .withCompletedAt(Instant.now().minusSeconds(3600))
                        .build());

        SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(testEntityFactory.faker())
                .forWorkoutSession(completedSession)
                .forExercise(exercise)
                .withPosition(1)
                .build();
        completedSession.addExerciseAt(sessionExercise, 1);
        workoutSessionRepository.saveAndFlush(completedSession);

        SessionSet sessionSet = SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(1)
                .withReps(10)
                .withWeightKg(50.0)
                .build();
        sessionExercise.addSet(sessionSet);
        sessionSetRepository.saveAndFlush(sessionSet);

        var result = sessionSetRepository.findLastSetsByExerciseIdAndUserId(Set.of(exercise.getId()), user.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExerciseId()).isEqualTo(exercise.getId());
        assertThat(result.get(0).getReps()).isEqualTo(10);
        assertThat(result.get(0).getWeightKg()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Should return empty when no completed sessions exist for exercise")
    void findLastSetsByExerciseIdAndUserId_ShouldReturnEmptyWhenNoCompletedSessions() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise =
                testEntityFactory.createAndPersistExercise(ExerciseBuilder.anExercise(testEntityFactory.faker()));

        WorkoutSession inProgressSession =
                workoutSessionRepository.saveAndFlush(WorkoutSessionBuilder.aWorkoutSession(testEntityFactory.faker())
                        .forUser(user)
                        .build());

        SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(testEntityFactory.faker())
                .forWorkoutSession(inProgressSession)
                .forExercise(exercise)
                .withPosition(1)
                .build();
        inProgressSession.addExerciseAt(sessionExercise, 1);
        workoutSessionRepository.saveAndFlush(inProgressSession);

        var result = sessionSetRepository.findLastSetsByExerciseIdAndUserId(Set.of(exercise.getId()), user.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when user differs")
    void findLastSetsByExerciseIdAndUserId_ShouldReturnEmptyWhenUserDiffers() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        Exercise exercise =
                testEntityFactory.createAndPersistExercise(ExerciseBuilder.anExercise(testEntityFactory.faker()));

        WorkoutSession completedSession =
                workoutSessionRepository.saveAndFlush(WorkoutSessionBuilder.aWorkoutSession(testEntityFactory.faker())
                        .forUser(owner)
                        .withStatus(dev.genesshoan.fitnesstrackerapi.workout.domain.SessionStatus.COMPLETED)
                        .withCompletedAt(Instant.now().minusSeconds(3600))
                        .build());

        SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(testEntityFactory.faker())
                .forWorkoutSession(completedSession)
                .forExercise(exercise)
                .withPosition(1)
                .build();
        completedSession.addExerciseAt(sessionExercise, 1);
        workoutSessionRepository.saveAndFlush(completedSession);

        SessionSet sessionSet = SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(1)
                .build();
        sessionExercise.addSet(sessionSet);
        sessionSetRepository.saveAndFlush(sessionSet);

        var result =
                sessionSetRepository.findLastSetsByExerciseIdAndUserId(Set.of(exercise.getId()), otherUser.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should update session exercise positions out of order with deferred constraint")
    @Transactional
    void shouldUpdatePositionsOutOfOrder_WithDeferredConstraint() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise =
                testEntityFactory.createAndPersistExercise(ExerciseBuilder.anExercise(testEntityFactory.faker()));
        Routine routine = testEntityFactory.createAndPersistRoutine(
                RoutineBuilder.aRoutine(testEntityFactory.faker()).forUser(user));
        WorkoutSession session =
                workoutSessionRepository.saveAndFlush(WorkoutSessionBuilder.aWorkoutSession(testEntityFactory.faker())
                        .forUser(user)
                        .withRoutine(routine)
                        .build());

        SessionExercise exercise1 = SessionExerciseBuilder.aSessionExercise(testEntityFactory.faker())
                .forWorkoutSession(session)
                .forExercise(exercise)
                .withPosition(1)
                .build();
        SessionExercise exercise2 = SessionExerciseBuilder.aSessionExercise(testEntityFactory.faker())
                .forWorkoutSession(session)
                .forExercise(exercise)
                .withPosition(2)
                .build();
        SessionExercise exercise3 = SessionExerciseBuilder.aSessionExercise(testEntityFactory.faker())
                .forWorkoutSession(session)
                .forExercise(exercise)
                .withPosition(3)
                .build();

        session.addExerciseAt(exercise1, 1);
        session.addExerciseAt(exercise2, 2);
        session.addExerciseAt(exercise3, 3);
        workoutSessionRepository.saveAndFlush(session);

        exercise2.setPosition(1);
        exercise3.setPosition(2);
        exercise1.setPosition(3);

        workoutSessionRepository.saveAndFlush(session);

        WorkoutSession refreshed =
                workoutSessionRepository.findById(session.getId()).orElseThrow();

        assertThat(refreshed.getExercises()).extracting("position").containsExactly(3, 1, 2);
    }
}
