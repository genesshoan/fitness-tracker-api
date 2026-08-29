package dev.genesshoan.fitnesstrackerapi.workout.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;

@Repository
public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    Page<WorkoutSession> findAllByUserId(UUID userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WorkoutSession> findForUpdateByIdAndUserId(UUID sessionId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ws
        FROM WorkoutSession ws
        JOIN FETCH ws.exercises
        WHERE ws.id = :id
            AND ws.user.id = :userId
        """)
    Optional<WorkoutSession> findForUpdateWithExercises(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
        SELECT DISTINCT ws
        FROM WorkoutSession ws
        JOIN FETCH ws.exercises e
        WHERE ws.id = :id
            AND ws.user.id = :userId
        """)
    Optional<WorkoutSession> findWithExercisesByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
