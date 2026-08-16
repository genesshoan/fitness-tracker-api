package dev.genesshoan.fitnesstrackerapi.workout.repository;

import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    Optional<WorkoutSession> findByIdAndUserId(UUID sessionId, UUID userId);

    Page<WorkoutSession> findAllByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"exercises"})
    Optional<WorkoutSession> findWithExercisesByIdAndUserId(UUID id, UUID userId);

    @EntityGraph(attributePaths = {"exercises", "exercises.sets"})
    Optional<WorkoutSession> findWithExercisesAndSetsByIdAndUserId(UUID id, UUID userId);
}
