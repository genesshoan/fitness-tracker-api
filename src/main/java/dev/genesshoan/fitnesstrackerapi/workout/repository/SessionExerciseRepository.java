package dev.genesshoan.fitnesstrackerapi.workout.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;

@Repository
public interface SessionExerciseRepository extends JpaRepository<SessionExercise, UUID> {

    @EntityGraph(attributePaths = {"workoutSession"})
    Optional<SessionExercise> findWithWorkoutSessionByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
            UUID id, UUID workoutSessionId, UUID userId);

    @EntityGraph(attributePaths = {"workoutSession", "exercise"})
    Optional<SessionExercise> findWithWorkoutSessionAndExerciseByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
            UUID id, UUID workoutSessionId, UUID userId);
}
