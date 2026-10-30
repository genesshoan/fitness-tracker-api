package dev.genesshoan.fitnesstrackerapi.workout.repository;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import dev.genesshoan.fitnesstrackerapi.base.AbstractPostgresTest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.testdata.TestEntityFactory;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionSetBuilder;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkoutSessionRepositoryTest extends AbstractPostgresTest {

    @Autowired
    WorkoutSessionRepository workoutSessionRepository;

    @Autowired
    SessionExerciseRepository sessionExerciseRepository;

    @Autowired
    SessionSetRepository sessionSetRepository;

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
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should confirm existence of a session owned by the user")
    void existsByIdAndUserId_ShouldReturnTrueWhenUserOwnsSession() {
        User user = testEntityFactory.createAndPersistUser();
        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(user);

        assertThat(workoutSessionRepository.existsByIdAndUserId(session.getId(), user.getId()))
                .isTrue();
    }

    @Test
    @DisplayName("Should not confirm existence of a session owned by another user")
    void existsByIdAndUserId_ShouldReturnFalseWhenUserDiffers() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(owner);

        assertThat(workoutSessionRepository.existsByIdAndUserId(session.getId(), otherUser.getId()))
                .isFalse();
    }

    @Test
    @DisplayName("Should not confirm existence of a session that does not exist")
    void existsByIdAndUserId_ShouldReturnFalseWhenSessionDoesNotExist() {
        User user = testEntityFactory.createAndPersistUser();

        assertThat(workoutSessionRepository.existsByIdAndUserId(UUID.randomUUID(), user.getId()))
                .isFalse();
    }

    @Test
    @DisplayName("Should load exercises and their exercise for update")
    void findForUpdateWithExercisesAndCatalogExercise_ShouldLoadExercisesAndSets() {
        User user = testEntityFactory.createAndPersistUser();
        WorkoutSession session = persistSessionWithExercises(user, 2);

        var result =
                workoutSessionRepository.findForUpdateWithExercisesAndCatalogExercise(session.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getExercises()).hasSize(2);
        assertThat(result.get().getExercises())
                .allSatisfy(sessionExercise ->
                        assertThat(sessionExercise.getExercise()).isNotNull());
        assertThat(result.get().getExercises())
                .allSatisfy(
                        sessionExercise -> assertThat(sessionExercise.getSets()).isNotEmpty());
    }

    @Test
    @DisplayName("Should return empty for findForUpdateWithExercisesAndCatalogExercise when user differs")
    void findForUpdateWithExercisesAndCatalogExercise_ShouldReturnEmptyWhenUserDiffers() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        WorkoutSession session = persistSessionWithExercises(owner, 1);

        var result = workoutSessionRepository.findForUpdateWithExercisesAndCatalogExercise(
                session.getId(), otherUser.getId());

        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should return empty for findForUpdateWithExercisesAndCatalogExercise when ids are missing")
    void findForUpdateWithExercisesAndCatalogExercise_ShouldReturnEmptyWhenIdsAreMissing() {
        var result = workoutSessionRepository.findForUpdateWithExercisesAndCatalogExercise(
                UUID.randomUUID(), UUID.randomUUID());

        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should load workout session with exercises for update")
    void findForUpdateWithExercises_ShouldLoadWorkoutSessionAndExercises() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        WorkoutSession session = persistSessionWithExercises(user, 2);

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
        WorkoutSession session = persistSessionWithExercises(owner, 1);

        var result = workoutSessionRepository.findForUpdateWithExercises(session.getId(), otherUser.getId());

        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should find a session without exercises for update")
    void findForUpdateWithExercises_ShouldFindSessionWithoutExercises() {
        User user = testEntityFactory.createAndPersistUser();
        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(user);

        var result = workoutSessionRepository.findForUpdateWithExercises(session.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(session.getId());
        assertThat(result.get().getExercises()).isEmpty();
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
        WorkoutSession session = persistSessionWithExercises(user, 2);

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

    private WorkoutSession persistSessionWithExercises(User user, int exerciseCount) {
        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(user);
        for (int i = 0; i < exerciseCount; i++) {
            Exercise exercise = testEntityFactory.createAndPersistExercise();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(testEntityFactory.faker())
                    .forWorkoutSession(session)
                    .forExercise(exercise)
                    .build();
            session.addExerciseAt(sessionExercise, session.getExercises().size() + 1);
            sessionExerciseRepository.saveAndFlush(sessionExercise);
            persistSessionSet(sessionExercise);
        }
        return session;
    }

    private SessionSet persistSessionSet(SessionExercise sessionExercise) {
        SessionSet sessionSet = SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(sessionExercise.getSetCount() + 1)
                .build();
        sessionExercise.addSet(sessionSet);
        return sessionSetRepository.saveAndFlush(sessionSet);
    }
}
