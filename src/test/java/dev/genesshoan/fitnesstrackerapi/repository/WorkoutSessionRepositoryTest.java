package dev.genesshoan.fitnesstrackerapi.repository;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import dev.genesshoan.fitnesstrackerapi.base.AbstractPostgresTest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.testdata.TestEntityFactory;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import dev.genesshoan.fitnesstrackerapi.workout.repository.WorkoutSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkoutSessionRepositoryTest extends AbstractPostgresTest {

    @Autowired
    WorkoutSessionRepository workoutSessionRepository;

    @Autowired
    TestEntityFactory testEntityFactory;

    @Test
    @DisplayName("Should find workout session by id and user id with pessimistic lock")
    void findForUpdateByIdAndUserId_ShouldReturnWorkoutSession() {
        User user = testEntityFactory.createAndPersistUser();
        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(user);

        var result = workoutSessionRepository.findForUpdateByIdAndUserId(session.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(session.getId());
    }

    @Test
    @DisplayName("Should return empty for findForUpdateByIdAndUserId when user differs")
    void findForUpdateByIdAndUserId_ShouldReturnEmptyWhenUserDiffers() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(owner);

        var result = workoutSessionRepository.findForUpdateByIdAndUserId(session.getId(), otherUser.getId());

        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should return empty for findForUpdateByIdAndUserId when ids are missing")
    void findForUpdateByIdAndUserId_ShouldReturnEmptyWhenIdsAreMissing() {
        var result = workoutSessionRepository.findForUpdateByIdAndUserId(UUID.randomUUID(), UUID.randomUUID());

        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should find all workout sessions for user with pagination")
    void findAllByUserId_ShouldReturnPagedSessions() {
        User user = testEntityFactory.createAndPersistUser();
        testEntityFactory.createAndPersistWorkoutSession(user);
        testEntityFactory.createAndPersistWorkoutSession(user);
        testEntityFactory.createAndPersistWorkoutSession(user);

        Pageable pageable = PageRequest.of(0, 2);

        var page = workoutSessionRepository.findAllByUserId(user.getId(), pageable);

        assertThat(page.getContent()).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should load workout session with exercises for update")
    void findForUpdateWithExercises_ShouldLoadWorkoutSessionAndExercises() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSessionWithExercises(user, 2);

        var result = workoutSessionRepository.findForUpdateWithExercises(session.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(session.getId());
        assertThat(result.get().getExercises()).hasSize(2);
    }

    @Test
    @DisplayName("Should return empty for findForUpdateWithExercises when user differs")
    void findForUpdateWithExercises_ShouldReturnEmptyWhenUserDiffers() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSessionWithExercises(owner, 1);

        var result = workoutSessionRepository.findForUpdateWithExercises(session.getId(), otherUser.getId());

        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should return empty for findForUpdateWithExercises when ids are missing")
    void findForUpdateWithExercises_ShouldReturnEmptyWhenIdsAreMissing() {
        var result = workoutSessionRepository.findForUpdateWithExercises(UUID.randomUUID(), UUID.randomUUID());

        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should load exercises when finding a session by id and owner")
    void findWithExercisesByIdAndUserId_ShouldLoadExercises() {
        User user = testEntityFactory.createAndPersistUser();
        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSessionWithExercises(user, 2);

        var result = workoutSessionRepository.findWithExercisesByIdAndUserId(session.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getExercises()).hasSize(2);
    }

    @Test
    @DisplayName("Should not load a session by id for another owner")
    void findWithExercisesByIdAndUserId_ShouldReturnEmptyWhenUserDiffers() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(owner);

        var result = workoutSessionRepository.findWithExercisesByIdAndUserId(session.getId(), otherUser.getId());

        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should find for update by id and user id with pessimistic lock")
    void findForUpdateByIdAndUserId_ShouldLoadWithPessimisticLock() {
        User user = testEntityFactory.createAndPersistUser();
        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(user);

        var result = workoutSessionRepository.findForUpdateByIdAndUserId(session.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(session.getId());
    }
}
