package dev.genesshoan.fitnesstrackerapi.exercise.muscle;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.Muscle;

public interface MuscleRepository extends JpaRepository<Muscle, UUID> {
    Optional<Muscle> findBySlug(String slug);
}
