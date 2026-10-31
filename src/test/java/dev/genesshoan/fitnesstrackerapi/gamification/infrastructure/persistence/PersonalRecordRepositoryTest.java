package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import dev.genesshoan.fitnesstrackerapi.base.AbstractPostgresTest;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.PersonalRecordMetric;
import dev.genesshoan.fitnesstrackerapi.testdata.TestEntityFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PersonalRecordRepositoryTest extends AbstractPostgresTest {

    @Autowired
    PersonalRecordRepository personalRecordRepository;

    @Autowired
    TestEntityFactory testEntityFactory;

    @Autowired
    TestEntityManager entityManager;

    @Test
    @DisplayName("Should create the record on the first value")
    void upsert_ShouldCreateTheRecordOnTheFirstValue() {
        // Given
        var user = testEntityFactory.createAndPersistUser();
        var exercise = testEntityFactory.createAndPersistExercise();

        // When
        personalRecordRepository.upsert(
                UUID.randomUUID(),
                user.getId(),
                exercise.getId(),
                PersonalRecordMetric.MAX_WEIGHT,
                null,
                80.0,
                null,
                null,
                Instant.now());
        entityManager.flush();
        entityManager.clear();

        // Then
        var result = personalRecordRepository.findByUserIdAndExerciseIdAndMetricAndWeight(
                user.getId(), exercise.getId(), PersonalRecordMetric.MAX_WEIGHT, null);
        assertThat(result).isPresent();
        assertThat(result.get().getValue()).isEqualTo(80.0);
    }

    @Test
    @DisplayName("Should keep the best value and the training that set it")
    void upsert_ShouldKeepTheBestValueAndItsSource() {
        // Given
        var user = testEntityFactory.createAndPersistUser();
        var exercise = testEntityFactory.createAndPersistExercise();
        var session = testEntityFactory.createAndPersistWorkoutSession(user);
        var sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        var set = testEntityFactory.createAndPersistSessionSet(sessionExercise);
        personalRecordRepository.upsert(
                UUID.randomUUID(),
                user.getId(),
                exercise.getId(),
                PersonalRecordMetric.MAX_WEIGHT,
                null,
                100.0,
                set.getId(),
                session.getId(),
                Instant.now());

        // When
        personalRecordRepository.upsert(
                UUID.randomUUID(),
                user.getId(),
                exercise.getId(),
                PersonalRecordMetric.MAX_WEIGHT,
                null,
                90.0,
                null,
                null,
                Instant.now());
        entityManager.flush();
        entityManager.clear();

        // Then
        var result = personalRecordRepository.findByUserIdAndExerciseIdAndMetricAndWeight(
                user.getId(), exercise.getId(), PersonalRecordMetric.MAX_WEIGHT, null);
        assertThat(result).isPresent();
        assertThat(result.get().getValue()).isEqualTo(100.0);
        assertThat(result.get().getSetId()).isEqualTo(set.getId());
        assertThat(personalRecordRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Should replace the source when the value improves")
    void upsert_ShouldReplaceTheSourceWhenTheValueImproves() {
        // Given
        var user = testEntityFactory.createAndPersistUser();
        var exercise = testEntityFactory.createAndPersistExercise();
        var session = testEntityFactory.createAndPersistWorkoutSession(user);
        var sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        var firstSet = testEntityFactory.createAndPersistSessionSet(sessionExercise);
        var secondSet = testEntityFactory.createAndPersistSessionSet(sessionExercise);
        personalRecordRepository.upsert(
                UUID.randomUUID(),
                user.getId(),
                exercise.getId(),
                PersonalRecordMetric.MAX_WEIGHT,
                null,
                100.0,
                firstSet.getId(),
                session.getId(),
                Instant.now());

        // When
        personalRecordRepository.upsert(
                UUID.randomUUID(),
                user.getId(),
                exercise.getId(),
                PersonalRecordMetric.MAX_WEIGHT,
                null,
                110.0,
                secondSet.getId(),
                session.getId(),
                Instant.now());
        entityManager.flush();
        entityManager.clear();

        // Then
        var result = personalRecordRepository.findByUserIdAndExerciseIdAndMetricAndWeight(
                user.getId(), exercise.getId(), PersonalRecordMetric.MAX_WEIGHT, null);
        assertThat(result).isPresent();
        assertThat(result.get().getValue()).isEqualTo(110.0);
        assertThat(result.get().getSetId()).isEqualTo(secondSet.getId());
    }

    @Test
    @DisplayName("Should keep one record per weight for the repetitions metric")
    void upsert_ShouldKeepOneRecordPerWeight() {
        // Given
        var user = testEntityFactory.createAndPersistUser();
        var exercise = testEntityFactory.createAndPersistExercise();

        // When
        personalRecordRepository.upsert(
                UUID.randomUUID(),
                user.getId(),
                exercise.getId(),
                PersonalRecordMetric.MAX_REPS_AT_WEIGHT,
                60.0,
                10.0,
                null,
                null,
                Instant.now());
        personalRecordRepository.upsert(
                UUID.randomUUID(),
                user.getId(),
                exercise.getId(),
                PersonalRecordMetric.MAX_REPS_AT_WEIGHT,
                80.0,
                6.0,
                null,
                null,
                Instant.now());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(personalRecordRepository.findAll()).hasSize(2);
        assertThat(personalRecordRepository.findByUserIdAndExerciseIdAndMetricAndWeight(
                        user.getId(), exercise.getId(), PersonalRecordMetric.MAX_REPS_AT_WEIGHT, 60.0))
                .isPresent()
                .get()
                .extracting(record -> record.getValue())
                .isEqualTo(10.0);
        assertThat(personalRecordRepository.findByUserIdAndExerciseIdAndMetricAndWeight(
                        user.getId(), exercise.getId(), PersonalRecordMetric.MAX_REPS_AT_WEIGHT, 80.0))
                .isPresent()
                .get()
                .extracting(record -> record.getValue())
                .isEqualTo(6.0);
    }

    @Test
    @DisplayName("Should remove the records of an exercise")
    void deleteByUserIdAndExerciseId_ShouldRemoveTheRecordsOfTheExercise() {
        // Given
        var user = testEntityFactory.createAndPersistUser();
        var exercise = testEntityFactory.createAndPersistExercise();
        personalRecordRepository.upsert(
                UUID.randomUUID(),
                user.getId(),
                exercise.getId(),
                PersonalRecordMetric.MAX_WEIGHT,
                null,
                80.0,
                null,
                null,
                Instant.now());
        entityManager.flush();
        entityManager.clear();

        // When
        var removed = personalRecordRepository.deleteByUserIdAndExerciseId(user.getId(), exercise.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(removed).isEqualTo(1);
        assertThat(personalRecordRepository.findByUserIdAndExerciseId(user.getId(), exercise.getId()))
                .isEmpty();
    }
}
