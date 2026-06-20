package dev.genesshoan.fitnesstrackerapi.exercise.muscle;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.Muscle;

import java.util.Optional;

public interface MuscleRepository extends JpaRepository<Muscle, Long> {

    Optional<Muscle> findBySlug(String slug);
}
