package dev.genesshoan.fitnesstrackerapi.repository;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

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
    @DisplayName("Should load workout session and exercise with session set")
    void
            findByIdAndSessionExerciseIdAndSessionExerciseWorkoutSessionIdAndSessionExerciseWorkoutSessionUserId_ShouldLoadAssociations() {
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
        session.addExercise(sessionExercise);
        workoutSessionRepository.saveAndFlush(session);

        SessionSet sessionSet = SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(1)
                .build();
        sessionExercise.addSet(sessionSet);
        sessionSetRepository.saveAndFlush(sessionSet);

        var result =
                sessionSetRepository
                        .findByIdAndSessionExerciseIdAndSessionExerciseWorkoutSessionIdAndSessionExerciseWorkoutSessionUserId(
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
    }

    @Test
    @DisplayName("Should return empty when workout session belongs to another user")
    void
            findByIdAndSessionExerciseIdAndSessionExerciseWorkoutSessionIdAndSessionExerciseWorkoutSessionUserId_ShouldReturnEmptyWhenUserDiffers() {
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
        session.addExercise(sessionExercise);
        workoutSessionRepository.saveAndFlush(session);

        SessionSet sessionSet = SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(1)
                .build();
        sessionExercise.addSet(sessionSet);
        sessionSetRepository.saveAndFlush(sessionSet);

        var result =
                sessionSetRepository
                        .findByIdAndSessionExerciseIdAndSessionExerciseWorkoutSessionIdAndSessionExerciseWorkoutSessionUserId(
                                sessionSet.getId(), sessionExercise.getId(), session.getId(), otherUser.getId());

        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should return empty when ids do not exist")
    void
            findByIdAndSessionExerciseIdAndSessionExerciseWorkoutSessionIdAndSessionExerciseWorkoutSessionUserId_ShouldReturnEmptyWhenIdsAreMissing() {
        var result =
                sessionSetRepository
                        .findByIdAndSessionExerciseIdAndSessionExerciseWorkoutSessionIdAndSessionExerciseWorkoutSessionUserId(
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
        session.addExercise(sessionExercise);
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
}
