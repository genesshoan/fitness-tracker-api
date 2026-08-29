package dev.genesshoan.fitnesstrackerapi.workout.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;
import dev.genesshoan.fitnesstrackerapi.workout.repository.projection.LastSetProjection;

public interface SessionSetRepository extends JpaRepository<SessionSet, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ss
        FROM SessionSet ss
        JOIN FETCH ss.sessionExercise se
        JOIN FETCH se.workoutSession ws
        JOIN FETCH se.exercise e
        WHERE ss.id = :sessionSetId
            AND se.id = :sessionExerciseId
            AND ws.id = :workoutSessionId
            AND ws.user.id = :userId
        """)
    Optional<SessionSet> findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
            @Param("sessionSetId") UUID sessionSetId,
            @Param("sessionExerciseId") UUID sessionExerciseId,
            @Param("workoutSessionId") UUID workoutSessionId,
            @Param("userId") UUID userId);

    @Query("""
            SELECT COALESCE(MAX(ss.setNumber), 0)
            FROM SessionSet ss
            WHERE ss.sessionExercise.id = :sessionExerciseId
        """)
    int findMaxSetNumberBySessionExerciseId(@Param("sessionExerciseId") UUID sessionExerciseId);

    @Query(value = """
        SELECT DISTINCT ON (e.id)
            e.id AS exerciseId,
            ss.reps AS reps,
            ss.weightKg AS weightKg,
            ss.durationSeconds AS durationSeconds,
            ss.distanceKm AS distanceKm
        FROM session_sets ss
        JOIN session_exercises se ON se.id = ss.session_exercise_id
        JOIN exercises e ON e.id = se.exercise_id
        JOIN workout_sessions ws ON ws.id = se.session_id
        WHERE e.id IN (:exerciseIds)
            AND ws.user_id = :userId
            AND ws.completed_at IS NOT NULL
        ORDER BY e.id, ws.completed_at DESC, ss.set_number DESC
        """, nativeQuery = true)
    List<LastSetProjection> findLastSetsByExerciseIdAndUserId(
            @Param("exerciseIds") Set<UUID> exerciseIds, @Param("userId") UUID userId);
}
