package dev.genesshoan.fitnesstrackerapi.exercise;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ExerciseMuscle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    Optional<Exercise> findBySlug(String slug);

    @Query(
        """
            SELECT e
            FROM Exercise e
            WHERE (:category IS NULL OR e.category = :category)
            AND (:difficulty IS NULL OR e.difficulty = :difficulty)
            AND (:muscleSlugs IS NULL OR EXISTS (
                SELECT 1
                FROM e.exerciseMuscles em
                WHERE em.muscle.slug IN :muscleSlugs
            ))
        """
    )
    Page<Exercise> findByFilters(
        @Param("category") Category category,
        @Param("difficulty") Difficulty difficulty,
        @Param("muscleSlugs") List<String> muscleSlugs,
        Pageable pageable
    );

    @Query(
        """
            SELECT em
            FROM ExerciseMuscle em
            JOIN FETCH em.muscle
            WHERE em.exercise.slug = :exerciseSlug
            ORDER BY
            CASE em.impactLevel
                WHEN PRIMARY THEN 1
                WHEN SECONDARY THEN 2
                WHEN STABILIZER THEN 3
            END ASC
        """
    )
    List<ExerciseMuscle> findMusclesByExerciseSlugOrderByImpactLevelASC(
        @Param("exerciseSlug") String exerciseSlug
    );
}
