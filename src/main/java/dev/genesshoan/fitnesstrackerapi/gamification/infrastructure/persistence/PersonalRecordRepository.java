package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.genesshoan.fitnesstrackerapi.gamification.domain.PersonalRecord;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.PersonalRecordMetric;

/**
 * Persistence operations for the personal records of the users.
 */
@Repository
public interface PersonalRecordRepository extends JpaRepository<PersonalRecord, UUID> {

    /**
     * Finds the record of one metric for one exercise.
     *
     * <p>The weight is {@code null} for the metrics that are not compared per
     * weight.</p>
     *
     * @param userId the user the record belongs to
     * @param exerciseId the exercise the record was set on
     * @param metric the metric the record was set for
     * @param weightKg the weight the record belongs to, or {@code null} for the
     *        metrics that are not compared per weight
     * @return the matching record, or {@link Optional#empty()} if the user has
     *         no such record yet
     */
    @Query("""
            SELECT pr
            FROM PersonalRecord pr
            WHERE pr.userId = :userId
                AND pr.exerciseId = :exerciseId
                AND pr.metric = :metric
                AND ((:weightKg IS NULL AND pr.weightKg IS NULL) OR pr.weightKg = :weightKg)
            """)
    Optional<PersonalRecord> findByUserIdAndExerciseIdAndMetricAndWeight(
            @Param("userId") UUID userId,
            @Param("exerciseId") UUID exerciseId,
            @Param("metric") PersonalRecordMetric metric,
            @Param("weightKg") Double weightKg);

    /**
     * Finds all the records of one exercise.
     *
     * @param userId the user the records belong to
     * @param exerciseId the exercise the records were set on
     * @return the records of the exercise
     */
    List<PersonalRecord> findByUserIdAndExerciseId(UUID userId, UUID exerciseId);

    /**
     * Finds all the records of one metric for one user.
     *
     * @param userId the user the records belong to
     * @param metric the metric the records were set for
     * @return the records of the metric
     */
    List<PersonalRecord> findByUserIdAndMetric(UUID userId, PersonalRecordMetric metric);

    /**
     * Records the value reached by a user on an exercise, keeping the best one.
     *
     * <p>The operation is idempotent: the same call twice leaves one record
     * with the best value. The record keeps pointing at the set that set the
     * best value, so a lower value never replaces its source.</p>
     *
     * @param id the identifier of the row to create
     * @param userId the user the record belongs to
     * @param exerciseId the exercise the record was set on
     * @param metric the metric the record was set for
     * @param weightKg the weight the record belongs to, or {@code null} for the
     *        metrics that are not compared per weight
     * @param value the value that was reached
     * @param setId the set that reached the value
     * @param sessionId the session that reached the value
     * @param achievedAt the instant the value was reached
     */
    default void upsert(
            UUID id,
            UUID userId,
            UUID exerciseId,
            PersonalRecordMetric metric,
            Double weightKg,
            Double value,
            UUID setId,
            UUID sessionId,
            Instant achievedAt) {
        upsertWithMetricName(id, userId, exerciseId, metric.name(), weightKg, value, setId, sessionId, achievedAt);
    }

    /**
     * Records the value reached by a user on an exercise.
     *
     * <p>The metric travels as its name because a native query binds an enum
     * parameter by its ordinal, which the database does not accept.</p>
     *
     * @param id the identifier of the row to create
     * @param userId the user the record belongs to
     * @param exerciseId the exercise the record was set on
     * @param metric the name of the metric the record was set for
     * @param weightKg the weight the record belongs to, or {@code null} for the
     *        metrics that are not compared per weight
     * @param value the value that was reached
     * @param setId the set that reached the value
     * @param sessionId the session that reached the value
     * @param achievedAt the instant the value was reached
     */
    @Modifying
    @Query(value = """
                    INSERT INTO personal_records (
                        id, user_id, exercise_id, metric, weight_kg, value,
                        set_id, session_id, achieved_at, created_at, updated_at)
                    VALUES (
                        :id, :userId, :exerciseId, :metric, :weightKg, :value,
                        :setId, :sessionId, :achievedAt, NOW(), NOW())
                    ON CONFLICT (user_id, exercise_id, metric, weight_kg) DO UPDATE
                    SET value = GREATEST(personal_records.value, EXCLUDED.value),
                        set_id = CASE
                            WHEN EXCLUDED.value > personal_records.value THEN EXCLUDED.set_id
                            ELSE personal_records.set_id
                        END,
                        session_id = CASE
                            WHEN EXCLUDED.value > personal_records.value THEN EXCLUDED.session_id
                            ELSE personal_records.session_id
                        END,
                        achieved_at = CASE
                            WHEN EXCLUDED.value > personal_records.value THEN EXCLUDED.achieved_at
                            ELSE personal_records.achieved_at
                        END,
                        updated_at = NOW()
                    """, nativeQuery = true)
    void upsertWithMetricName(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("exerciseId") UUID exerciseId,
            @Param("metric") String metric,
            @Param("weightKg") Double weightKg,
            @Param("value") Double value,
            @Param("setId") UUID setId,
            @Param("sessionId") UUID sessionId,
            @Param("achievedAt") Instant achievedAt);

    /**
     * Removes the records of one exercise.
     *
     * @param userId the user the records belong to
     * @param exerciseId the exercise the records were set on
     * @return the number of records removed
     */
    @Modifying
    @Query("DELETE FROM PersonalRecord pr WHERE pr.userId = :userId AND pr.exerciseId = :exerciseId")
    int deleteByUserIdAndExerciseId(@Param("userId") UUID userId, @Param("exerciseId") UUID exerciseId);
}
