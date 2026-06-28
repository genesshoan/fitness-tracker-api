package dev.genesshoan.fitnesstrackerapi.exercise;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {
    @EntityGraph(
        attributePaths = { "exerciseMuscles", "exerciseMuscles.muscle" }
    )
    Optional<Exercise> findBySlugAndActiveTrue(String slug);

    @Query(
        """
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
        """
    )
    List<Exercise> findByFilters(
        @Param("cursor") UUID cursor,
        @Param("category") Category category,
        @Param("difficulty") Difficulty difficulty,
        @Param("muscleSlugs") List<String> muscleSlugs,
        Pageable pageable
    );
}
