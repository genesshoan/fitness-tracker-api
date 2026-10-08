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

import dev.genesshoan.fitnesstrackerapi.exercise.application.ports.inbound.ExerciseQueryPort;
import dev.genesshoan.fitnesstrackerapi.exercise.application.ports.outbound.ExerciseRepositoryPort;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;

/**
 * Persistence operations for exercises, including active-catalog queries and soft deletion.
 */
@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, UUID>, ExerciseRepositoryPort, ExerciseQueryPort {

    /**
     * {@inheritDoc}
     */
    @Override
    default Exercise saveExercise(Exercise exercise) {
        return save(exercise);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @EntityGraph(attributePaths = {"exerciseMuscles", "exerciseMuscles.muscle"})
    Optional<Exercise> findBySlugAndActiveTrue(String slug);

    /**
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    List<Exercise> findAllByIdInAndActiveTrue(Collection<UUID> ids);

    /**
     * {@inheritDoc}
     */
    @Override
    Optional<Exercise> findByIdAndActiveTrue(UUID id);

    /**
     * {@inheritDoc}
     */
    @Override
    boolean existsBySlug(String slug);

    /**
     * {@inheritDoc}
     */
    @Override
    @Modifying
    @Query("UPDATE Exercise e SET e.active = false, e.updatedAt = CURRENT_TIMESTAMP"
            + " WHERE e.slug = :slug AND e.active = true")
    int softDeleteBySlug(@Param("slug") String slug);
}
