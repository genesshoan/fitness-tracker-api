package dev.genesshoan.fitnesstrackerapi.exercise.application.ports.outbound;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.Muscle;

/**
 * Outbound port for muscle persistence operations.
 *
 * <p>Infrastructure adapters implement this contract so that application
 * services do not depend on a specific persistence technology.
 */
public interface MuscleRepositoryPort {

    /**
     * Finds muscles whose slugs are contained in the specified list.
     *
     * @param slugs the slugs identifying the muscles to find
     * @return a list containing the muscles matching the specified slugs
     */
    List<Muscle> findBySlugIn(List<String> slugs);

    /**
     * Finds a muscle by its unique slug.
     *
     * @param slug the slug identifying the muscle
     * @return the matching muscle, or {@link Optional#empty()} if no muscle
     *         with the given slug exists
     */
    Optional<Muscle> findBySlug(String slug);

    /**
     * Finds all muscles.
     *
     * @return a list containing all muscles
     */
    Page<Muscle> findAll(Pageable pageable);

    /**
     * Persists the specified muscle.
     *
     * @param muscle the muscle to persist
     * @return the persisted muscle
     */
    Muscle saveMuscle(Muscle muscle);
}
