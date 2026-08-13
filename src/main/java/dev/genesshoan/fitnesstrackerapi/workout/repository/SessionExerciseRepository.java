package dev.genesshoan.fitnesstrackerapi.workout.repository;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionExerciseRepository extends JpaRepository<SessionExercise, UUID> {}
