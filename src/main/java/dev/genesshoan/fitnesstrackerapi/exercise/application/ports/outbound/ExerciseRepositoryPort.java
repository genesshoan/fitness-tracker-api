package dev.genesshoan.fitnesstrackerapi.exercise.application.ports.outbound;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;

/**
 * Outbound port for exercise persistence operations.
 *
 * <p>Infrastructure adapters (e.g., JPA repositories) implement this contract
 * so that application services do not depend on a specific persistence
 * technology.
 */
public interface ExerciseRepositoryPort {

    /**
     * Finds an active exercise by its unique slug.
     *
     * @param slug the slug identifying the exercise
     * @return the matching active exercise, or {@link Optional#empty()} if no
     *         active exercise with the given slug exists
     */
    Optional<Exercise> findBySlugAndActiveTrue(String slug);

    /**
     * Finds active exercises matching the specified filters using cursor-based
     * pagination.
     *
     * @param cursor the identifier used as the pagination cursor; {@code null}
     *        starts from the beginning
     * @param category the exercise category to filter by, or {@code null} to
     *        include all categories
     * @param difficulty the exercise difficulty to filter by, or {@code null}
     *        to include all difficulties
     * @param muscleSlugs the muscle slugs to filter by; an empty collection
     *        means no muscle filter is applied
     * @param pageable pagination and sorting parameters
     * @return a list of active exercises matching the specified filters
     */
    List<Exercise> findByFiltersAndActiveTrue(
            UUID cursor, Category category, Difficulty difficulty, List<String> muscleSlugs, Pageable pageable);

    /**
     * Searches active exercises matching the specified query.
     *
     * @param query the search text
     * @param limit the maximum number of exercises to return
     * @return a list of matching active exercises, limited to the specified
     *         number of results
     */
    List<Exercise> searchActive(String query, int limit);

    /**
     * Finds all active exercises whose identifiers are contained in the
     * specified set.
     *
     * @param ids the identifiers of the exercises to find
     * @return the active exercises whose identifiers are contained in
     *         {@code ids}
     */
    List<Exercise> findAllByIdInAndActiveTrue(Collection<UUID> ids);

    /**
     * Finds an active exercise by its identifier.
     *
     * @param id the identifier of the exercise
     * @return the matching active exercise, or {@link Optional#empty()} if no
     *         active exercise with the given identifier exists
     */
    Optional<Exercise> findByIdAndActiveTrue(UUID id);

    /**
     * Checks whether an exercise with the specified slug exists.
     *
     * <p>This method does not restrict the check to active exercises.</p>
     *
     * @param slug the slug to check
     * @return {@code true} if an exercise with the specified slug exists,
     *         {@code false} otherwise
     */
    boolean existsBySlug(String slug);

    /**
     * Persists the specified exercise.
     *
     * @param exercise the exercise to persist
     * @return the persisted exercise
     */
    Exercise saveExercise(Exercise exercise);

    /**
     * Soft-deletes an exercise identified by its slug.
     *
     * <p>The exercise is not physically removed from persistence.</p>
     *
     * @param slug the slug identifying the exercise
     * @return the number of exercises affected by the operation
     */
    int softDeleteBySlug(String slug);
}
