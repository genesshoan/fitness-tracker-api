package dev.genesshoan.fitnesstrackerapi.repository;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import dev.genesshoan.fitnesstrackerapi.base.AbstractPostgresTest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.testdata.TestEntityFactory;
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
    void findForUpdateWithWorkoutSessionAndExerciseAndSets_ShouldLoadAssociations() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        Routine routine = testEntityFactory.createAndPersistRoutine(user);

        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(user, routine);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(sessionExercise);

        var result = sessionExerciseRepository.findForUpdateWithWorkoutSessionAndExerciseAndSets(
                sessionExercise.getId(), session.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getWorkoutSession().getId()).isEqualTo(session.getId());
        assertThat(result.get().getWorkoutSession().getUser().getId()).isEqualTo(user.getId());
        assertThat(result.get().getExercise().getId()).isEqualTo(exercise.getId());
    }

    @Test
    @DisplayName("Should return empty when workout session belongs to another user")
    void findForUpdateWithWorkoutSessionAndExerciseAndSets_ShouldReturnEmptyWhenUserDiffers() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        Routine routine = testEntityFactory.createAndPersistRoutine(owner);

        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(owner, routine);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(sessionExercise);

        var result = sessionExerciseRepository.findForUpdateWithWorkoutSessionAndExerciseAndSets(
                sessionExercise.getId(), session.getId(), otherUser.getId());

        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should return empty when ids do not exist")
    void findForUpdateWithWorkoutSessionAndExerciseAndSets_ShouldReturnEmptyWhenIdsAreMissing() {
        var result = sessionExerciseRepository.findForUpdateWithWorkoutSessionAndExerciseAndSets(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should load workout session when querying by ownership with sets")
    void findForUpdateWithWorkoutSessionAndSets_ShouldLoadWorkoutSession() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        Routine routine = testEntityFactory.createAndPersistRoutine(user);

        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(user, routine);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(sessionExercise);

        var result = sessionExerciseRepository.findForUpdateWithWorkoutSessionAndSets(
                sessionExercise.getId(), session.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getWorkoutSession().getId()).isEqualTo(session.getId());
        assertThat(result.get().getWorkoutSession().getUser().getId()).isEqualTo(user.getId());
        // Verify that sets were fetched (JOIN FETCH se.sets)
        assertThat(result.get().getSets()).isNotEmpty();
    }

    @Test
    @DisplayName("Should load workout session only when querying with findForUpdateWithWorkoutSession")
    void findForUpdateWithWorkoutSession_ShouldLoadOnlyWorkoutSession() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        Routine routine = testEntityFactory.createAndPersistRoutine(user);

        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(user, routine);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);

        var result = sessionExerciseRepository.findForUpdateWithWorkoutSession(
                sessionExercise.getId(), session.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getWorkoutSession().getId()).isEqualTo(session.getId());
        assertThat(result.get().getWorkoutSession().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("Should return empty for findForUpdateWithWorkoutSession when user differs")
    void findForUpdateWithWorkoutSession_ShouldReturnEmptyWhenUserDiffers() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        Routine routine = testEntityFactory.createAndPersistRoutine(owner);

        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(owner, routine);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);

        var result = sessionExerciseRepository.findForUpdateWithWorkoutSession(
                sessionExercise.getId(), session.getId(), otherUser.getId());

        assertThat(result).isNotPresent();
    }
}
