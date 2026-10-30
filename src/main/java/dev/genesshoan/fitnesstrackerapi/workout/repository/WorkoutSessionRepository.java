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

    boolean existsByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WorkoutSession> findForUpdateByIdAndUserId(UUID sessionId, UUID userId);

    /**
     * Locks the session and loads its exercises.
     *
     * <p>The join has to be an outer one, a session without exercises is a
     * valid session and the first exercise is added to it right after it is
     * created. An inner join would hide those sessions and make them look
     * like they did not exist.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT DISTINCT ws
        FROM WorkoutSession ws
        LEFT JOIN FETCH ws.exercises
        WHERE ws.id = :id
            AND ws.user.id = :userId
        """)
    Optional<WorkoutSession> findForUpdateWithExercises(@Param("id") UUID id, @Param("userId") UUID userId);

    /**
     * Locks the session and loads its exercises together with the catalog exercise of
     * each one.
     *
     * <p>The sets of an exercise are not fetched, a collection bag cannot be fetched
     * together with the exercises bag, so they are loaded lazily when they are read.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT DISTINCT ws
        FROM WorkoutSession ws
        LEFT JOIN FETCH ws.exercises e
        LEFT JOIN FETCH e.exercise
        WHERE ws.id = :id
            AND ws.user.id = :userId
        """)
    Optional<WorkoutSession> findForUpdateWithExercisesAndCatalogExercise(
            @Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
        SELECT DISTINCT ws
        FROM WorkoutSession ws
        LEFT JOIN FETCH ws.exercises e
        WHERE ws.id = :id
            AND ws.user.id = :userId
        """)
    Optional<WorkoutSession> findWithExercisesByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
