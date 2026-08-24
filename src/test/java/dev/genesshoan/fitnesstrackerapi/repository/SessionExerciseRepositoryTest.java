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
import dev.genesshoan.fitnesstrackerapi.testdata.builder.WorkoutSessionBuilder;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import dev.genesshoan.fitnesstrackerapi.workout.repository.SessionExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.workout.repository.WorkoutSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SessionExerciseRepositoryTest extends AbstractPostgresTest {

    @Autowired
    SessionExerciseRepository sessionExerciseRepository;

    @Autowired
    WorkoutSessionRepository workoutSessionRepository;

    @Autowired
    TestEntityFactory testEntityFactory;

    @Test
    @DisplayName("Should load workout session and exercise with session exercise")
    void findWithWorkoutSessionAndExerciseByIdAndWorkoutSessionIdAndWorkoutSessionUserId_ShouldLoadAssociations() {
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

        var result =
                sessionExerciseRepository
                        .findWithWorkoutSessionAndExerciseByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
                                sessionExercise.getId(), session.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getWorkoutSession().getId()).isEqualTo(session.getId());
        assertThat(result.get().getWorkoutSession().getUser().getId()).isEqualTo(user.getId());
        assertThat(result.get().getExercise().getId()).isEqualTo(exercise.getId());
    }

    @Test
    @DisplayName("Should return empty when workout session belongs to another user")
    void
            findWithWorkoutSessionAndExerciseByIdAndWorkoutSessionIdAndWorkoutSessionUserId_ShouldReturnEmptyWhenUserDiffers() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        Exercise exercise =
                testEntityFactory.createAndPersistExercise(ExerciseBuilder.anExercise(testEntityFactory.faker()));
        Routine routine = testEntityFactory.createAndPersistRoutine(
                RoutineBuilder.aRoutine(testEntityFactory.faker()).forUser(owner));
        WorkoutSession session =
                workoutSessionRepository.saveAndFlush(WorkoutSessionBuilder.aWorkoutSession(testEntityFactory.faker())
                        .forUser(owner)
                        .withRoutine(routine)
                        .build());

        SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(testEntityFactory.faker())
                .forWorkoutSession(session)
                .forExercise(exercise)
                .build();
        session.addExercise(sessionExercise);
        workoutSessionRepository.saveAndFlush(session);

        var result =
                sessionExerciseRepository
                        .findWithWorkoutSessionAndExerciseByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
                                sessionExercise.getId(), session.getId(), otherUser.getId());

        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should return empty when ids do not exist")
    void
            findWithWorkoutSessionAndExerciseByIdAndWorkoutSessionIdAndWorkoutSessionUserId_ShouldReturnEmptyWhenIdsAreMissing() {
        var result =
                sessionExerciseRepository
                        .findWithWorkoutSessionAndExerciseByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
                                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should load workout session when querying by ownership")
    void findWithWorkoutSessionByIdAndWorkoutSessionIdAndWorkoutSessionUserId_ShouldLoadWorkoutSession() {
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

        var result = sessionExerciseRepository.findWithWorkoutSessionByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
                sessionExercise.getId(), session.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getWorkoutSession().getId()).isEqualTo(session.getId());
        assertThat(result.get().getWorkoutSession().getUser().getId()).isEqualTo(user.getId());
    }
}
