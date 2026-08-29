package dev.genesshoan.fitnesstrackerapi.workout.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;

@Repository
public interface SessionExerciseRepository extends JpaRepository<SessionExercise, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT se
        FROM SessionExercise se
        JOIN FETCH se.workoutSession ws
        WHERE se.id = :id
            AND ws.id = :workoutSessionId
            AND ws.user.id = :userId
        """)
    Optional<SessionExercise> findForUpdateWithWorkoutSession(
            @Param("id") UUID id, @Param("workoutSessionId") UUID workoutSessionId, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT se
        FROM SessionExercise se
        JOIN FETCH se.workoutSession ws
        JOIN FETCH se.sets
        WHERE se.id = :id
            AND ws.id = :workoutSessionId
            AND ws.user.id = :userId
        """)
    Optional<SessionExercise> findForUpdateWithWorkoutSessionAndSets(
            @Param("id") UUID id, @Param("workoutSessionId") UUID workoutSessionId, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT se
        FROM SessionExercise se
        JOIN FETCH se.workoutSession ws
        JOIN FETCH se.exercise
        JOIN FETCH se.sets
        WHERE se.id = :id
            AND ws.id = :workoutSessionId
            AND ws.user.id = :userId
        """)
    Optional<SessionExercise> findForUpdateWithWorkoutSessionAndExerciseAndSets(
            @Param("id") UUID id, @Param("workoutSessionId") UUID workoutSessionId, @Param("userId") UUID userId);
}
