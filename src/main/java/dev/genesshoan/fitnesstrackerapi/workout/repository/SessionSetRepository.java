package dev.genesshoan.fitnesstrackerapi.workout.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;

public interface SessionSetRepository extends JpaRepository<SessionSet, UUID> {

    @EntityGraph(attributePaths = {"sessionExercise.workoutSession", "sessionExercise.exercise"})
    Optional<SessionSet>
            findByIdAndSessionExerciseIdAndSessionExerciseWorkoutSessionIdAndSessionExerciseWorkoutSessionUserId(
                    UUID sessionSetId, UUID sessionExerciseId, UUID workoutSessionId, UUID userId);

    @Query("""
            SELECT COALESCE(MAX(ss.setNumber), 0)
            FROM SessionSet ss
            WHERE ss.sessionExercise.id = :sessionExerciseId
        """)
    int findMaxSetNumberBySessionExerciseId(@Param("sessionExerciseId") UUID sessionExerciseId);
}
