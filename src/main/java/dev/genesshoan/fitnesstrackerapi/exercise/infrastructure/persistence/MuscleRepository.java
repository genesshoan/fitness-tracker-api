package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Muscle;

/**
 * Persistence operations for the muscle catalog.
 */
public interface MuscleRepository extends JpaRepository<Muscle, UUID> {

    /**
     * Finds a muscle by its unique slug.
     *
     * @param slug the slug identifying the muscle
     * @return the matching muscle, or {@link Optional#empty()} if no muscle
     *         with the given slug exists
     */
    Optional<Muscle> findBySlug(String slug);

    /**
     * Finds muscles whose slugs are contained in the specified list.
     *
     * @param slugs the slugs identifying the muscles to find
     * @return a list containing the muscles matching the specified slugs
     */
    List<Muscle> findBySlugIn(List<String> slugs);
}
