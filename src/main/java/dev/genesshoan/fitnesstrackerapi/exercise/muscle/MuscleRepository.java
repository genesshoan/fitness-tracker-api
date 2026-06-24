package dev.genesshoan.fitnesstrackerapi.exercise.muscle;

import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.Muscle;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MuscleRepository extends JpaRepository<Muscle, UUID> {
    Optional<Muscle> findBySlug(String slug);
}
