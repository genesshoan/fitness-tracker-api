package dev.genesshoan.fitnesstrackerapi.workout.repository;

import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {}
