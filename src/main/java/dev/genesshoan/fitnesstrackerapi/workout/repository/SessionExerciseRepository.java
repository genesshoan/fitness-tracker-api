package dev.genesshoan.fitnesstrackerapi.workout.repository;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionExerciseRepository extends JpaRepository<SessionExercise, UUID> {

    @EntityGraph(attributePaths = {"workoutSession"})
    Optional<SessionExercise> findWithWorkoutSessionByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
            UUID id, UUID workoutSessionId, UUID userId);

    @EntityGraph(attributePaths = {"sets"})
    Optional<SessionExercise> findWithSetsByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
            UUID id, UUID workoutSessionId, UUID userId);
}
