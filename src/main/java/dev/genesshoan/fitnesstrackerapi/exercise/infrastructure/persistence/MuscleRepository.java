package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.genesshoan.fitnesstrackerapi.exercise.application.ports.outbound.MuscleRepositoryPort;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.Muscle;

public interface MuscleRepository extends JpaRepository<Muscle, UUID>, MuscleRepositoryPort {

    /**
     * {@inheritDoc}
     */
    @Override
    default Muscle saveMuscle(Muscle muscle) {
        return save(muscle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Optional<Muscle> findBySlug(String slug);

    /**
     * {@inheritDoc}
     */
    @Override
    List<Muscle> findBySlugIn(List<String> slugs);
}
