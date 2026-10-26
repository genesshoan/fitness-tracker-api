package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;

/**
 * Persistence operations for exercises, including active-catalog queries and soft deletion.
 */
@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    /**
     * Finds an active exercise by its unique slug.
     *
     * @param slug the slug identifying the exercise
     * @return the matching active exercise, or {@link Optional#empty()} if no
     *         active exercise with the given slug exists
     */
    @EntityGraph(attributePaths = {"exerciseMuscles", "exerciseMuscles.muscle"})
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
    @Query("""
            SELECT e
            FROM Exercise e
            WHERE (:cursor IS NULL OR e.id > :cursor)
                AND e.active = true
                AND (:category IS NULL OR e.category = :category)
                AND (:difficulty IS NULL OR e.difficulty = :difficulty)
                AND (:muscleSlugs IS NULL OR EXISTS (
                    SELECT 1
                    FROM e.exerciseMuscles em
                    WHERE em.muscle.slug IN :muscleSlugs
                ))
            ORDER BY e.id ASC
        """)
    List<Exercise> findByFiltersAndActiveTrue(
            @Param("cursor") UUID cursor,
            @Param("category") Category category,
            @Param("difficulty") Difficulty difficulty,
            @Param("muscleSlugs") List<String> muscleSlugs,
            Pageable pageable);

    /**
     * Searches active exercises matching the specified query.
     *
     * @param query the search text
     * @param limit the maximum number of exercises to return
     * @return a list of matching active exercises, limited to the specified
     *         number of results
     */
    @Query(value = """
        SELECT e.*
        FROM exercises e
        WHERE e.active = true
            AND (e.name ILIKE '%' || :query || '%' ESCAPE '\\'
                OR e.slug ILIKE '%' || :query || '%' ESCAPE '\\')
        ORDER BY
            CASE WHEN e.name ILIKE :query || '%' ESCAPE '\\' THEN 0 ELSE 1 END,
            e.name ASC,
            e.id ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Exercise> searchActive(@Param("query") String query, @Param("limit") int limit);

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
     * Checks whether an active exercise exists with the specified identifier.
     *
     * @param id the identifier of the exercise
     * @return {@code true} if an active exercise with the specified identifier
     *         exists, {@code false} otherwise
     */
    boolean existsByIdAndActiveTrue(UUID id);

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
     * Soft-deletes an exercise identified by its slug.
     *
     * <p>The exercise is not physically removed from persistence.</p>
     *
     * @param slug the slug identifying the exercise
     * @return the number of exercises affected by the operation
     */
    @Modifying
    @Query("UPDATE Exercise e SET e.active = false, e.updatedAt = CURRENT_TIMESTAMP"
            + " WHERE e.slug = :slug AND e.active = true")
    int softDeleteBySlug(@Param("slug") String slug);
}
