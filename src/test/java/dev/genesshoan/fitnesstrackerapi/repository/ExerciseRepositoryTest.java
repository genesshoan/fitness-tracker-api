package dev.genesshoan.fitnesstrackerapi.repository;

import static org.assertj.core.api.Assertions.assertThat;

import dev.genesshoan.fitnesstrackerapi.base.AbstractPostgresTest;
import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.MuscleRepository;
import dev.genesshoan.fitnesstrackerapi.testdata.TestEntityFactory;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ExerciseBuilder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

public class ExerciseRepositoryTest extends AbstractPostgresTest {

    @Autowired
    ExerciseRepository exerciseRepository;

    @Autowired
    MuscleRepository muscleRepository;

    @Autowired
    TestEntityFactory testEntityFactory;

    @Test
    @DisplayName("Should return exercise by slug and active only")
    void findBySlugAndActiveTrue_ShouldReturnExerciseBySlugAndActiveOnly() {
        // Given
        var activeExercise = testEntityFactory.createAndPersistExercise();

        // When
        var result = exerciseRepository.findBySlugAndActiveTrue(
            activeExercise.getSlug()
        );

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSlug()).isEqualTo(activeExercise.getSlug());
    }

    @Test
    @DisplayName("Should not return exercise by slug when inactive")
    void findBySlugAndActiveTrue_ShouldNotReturnExerciseBySlugWhenInactive() {
        // Given
        var inactiveExercise = testEntityFactory.createAndPersistExercise(
            ExerciseBuilder.anExercise(testEntityFactory.faker()).withActive(
                false
            )
        );

        // When
        var inactiveResult = exerciseRepository.findBySlugAndActiveTrue(
            inactiveExercise.getSlug()
        );

        // Then
        assertThat(inactiveResult).isNotPresent();
    }

    @Test
    @DisplayName("Should return empty when no active exercise found by slug")
    void findBySlugAndActiveTrue_ShouldReturnEmptyWhenNoActiveExerciseFoundBySlug() {
        // When
        var result = exerciseRepository.findBySlugAndActiveTrue(
            "non-existent-slug"
        );

        // Then
        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Should fetch exercise with muscles")
    void findBySlugAndActiveTrue_ShouldFetchExerciseWithMuscles() {
        // Given
        var exercise = testEntityFactory.createAndPersistExerciseWithMuscles(
            3,
            ImpactLevel.SECONDARY
        );

        // When
        var result = exerciseRepository.findBySlugAndActiveTrue(
            exercise.getSlug()
        );

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getExerciseMuscles())
            .isNotEmpty()
            .allSatisfy(em -> assertThat(em.getMuscle()).isNotNull());
    }

    @Test
    @DisplayName("With no filters, should return all active exercises")
    void findBySlugAndActiveTrue_WithNoFilters_ShouldReturnAllActiveExercises() {
        // Given
        var exercises = new ArrayList<Exercise>();
        for (int i = 0; i < 10; i++) {
            exercises.add(testEntityFactory.createAndPersistExercise());
        }

        // When
        var result = exerciseRepository.findByFilters(
            null,
            null,
            null,
            null,
            PageRequest.of(0, 10)
        );

        // Then
        assertThat(result)
            .hasSize(exercises.size())
            .extracting(Exercise::getSlug)
            .containsExactlyInAnyOrderElementsOf(
                exercises
                    .stream()
                    .map(e -> ((Exercise) e).getSlug())
                    .toList()
            );
    }

    @Test
    @DisplayName("Should filter exercises by category")
    void findBySlugAndActiveTrue_WithCategoryFilter_ShouldFilterExercisesByCategory() {
        // Given

        // Category.STRENGTH by default
        for (int i = 0; i < 10; i++) {
            testEntityFactory.createAndPersistExercise();
        }

        var mobilityExercise = testEntityFactory.createAndPersistExercise(
            ExerciseBuilder.anExercise(testEntityFactory.faker()).withCategory(
                Category.MOBILITY
            )
        );

        // When
        var result = exerciseRepository.findByFilters(
            null,
            Category.MOBILITY,
            null,
            null,
            PageRequest.of(0, 20)
        );

        // Then
        assertThat(result).hasSize(1).containsExactly(mobilityExercise);
    }

    @Test
    @DisplayName("Should filter exercises by difficulty")
    void findBySlugAndActiveTrue_WithDifficultyFilter_ShouldFilterExercisesByDifficulty() {
        // Given

        // Difficulty.INTERMEDIATE by default
        for (int i = 0; i < 10; i++) {
            testEntityFactory.createAndPersistExercise();
        }

        var advancedExercise = testEntityFactory.createAndPersistExercise(
            ExerciseBuilder.anExercise(
                testEntityFactory.faker()
            ).withDifficulty(Difficulty.ADVANCED)
        );

        // When
        var result = exerciseRepository.findByFilters(
            null,
            null,
            Difficulty.ADVANCED,
            null,
            PageRequest.of(0, 20)
        );

        // Then
        assertThat(result).hasSize(1).containsExactly(advancedExercise);
    }

    @Test
    @DisplayName("Should filter exercises by difficulty and category")
    void findBySlugAndActiveTrue_WithDifficultyAndCategoryFilter_ShouldFilterExercisesByDifficultyAndCategory() {
        // Given

        // Category.STRENGTH by default
        // Difficulty.INTERMEDIATE by default
        for (int i = 0; i < 10; i++) {
            testEntityFactory.createAndPersistExercise();
        }

        var filteredExercise = testEntityFactory.createAndPersistExercise(
            ExerciseBuilder.anExercise(testEntityFactory.faker())
                .withDifficulty(Difficulty.ADVANCED)
                .withCategory(Category.CARDIO)
        );

        // When
        var result = exerciseRepository.findByFilters(
            null,
            Category.CARDIO,
            Difficulty.ADVANCED,
            null,
            PageRequest.of(0, 20)
        );

        // Then
        assertThat(result).hasSize(1).containsExactly(filteredExercise);
    }

    @Test
    @DisplayName("Should filter exercises by muscle slugs")
    void findByFilters_WithMuscleSlugs_ShouldFilterExercisesByMuscles() {
        // Given
        var chest = testEntityFactory.createAndPersistMuscle();
        var bicep = testEntityFactory.createAndPersistMuscle();

        var chestExercise =
            testEntityFactory.createAndPersistExerciseWithMuscles(
                ExerciseBuilder.anExercise(testEntityFactory.faker()),
                List.of(chest),
                ImpactLevel.PRIMARY
            );

        testEntityFactory.createAndPersistExerciseWithMuscles(
            ExerciseBuilder.anExercise(testEntityFactory.faker()),
            List.of(bicep),
            ImpactLevel.PRIMARY
        );

        // When
        var result = exerciseRepository.findByFilters(
            null,
            null,
            null,
            List.of(chest.getSlug()),
            PageRequest.of(0, 20)
        );

        // Then
        assertThat(result).hasSize(1).containsExactly(chestExercise);
    }

    @Test
    @DisplayName("Should combine all filters correctly")
    void findByFilters_WithAllFilters_ShouldReturnMatchingExercises() {
        // Given
        var chest = testEntityFactory.createAndPersistMuscle();

        var expected = testEntityFactory.createAndPersistExerciseWithMuscles(
            ExerciseBuilder.anExercise(testEntityFactory.faker())
                .withCategory(Category.STRENGTH)
                .withDifficulty(Difficulty.ADVANCED),
            List.of(chest),
            ImpactLevel.PRIMARY
        );

        // Noise
        testEntityFactory.createAndPersistExercise(
            ExerciseBuilder.anExercise(testEntityFactory.faker())
                .withCategory(Category.CARDIO)
                .withDifficulty(Difficulty.ADVANCED)
        );

        // When
        var result = exerciseRepository.findByFilters(
            null,
            Category.STRENGTH,
            Difficulty.ADVANCED,
            List.of(chest.getSlug()),
            PageRequest.of(0, 20)
        );

        // Then
        assertThat(result).hasSize(1).containsExactly(expected);
    }

    @Test
    @DisplayName("Should return exercises after cursor only")
    void findByFilters_WithCursor_ShouldReturnExercisesAfterCursor() {
        // Given
        var first = testEntityFactory.createAndPersistExercise();
        var second = testEntityFactory.createAndPersistExercise();
        var third = testEntityFactory.createAndPersistExercise();

        // When
        var result = exerciseRepository.findByFilters(
            first.getId(),
            null,
            null,
            null,
            PageRequest.of(0, 20)
        );

        // Then
        assertThat(result)
            .extracting(Exercise::getId)
            .containsExactly(second.getId(), third.getId());
    }

    @Test
    @DisplayName("Should not return inactive exercises")
    void findByFilters_ShouldExcludeInactiveExercises() {
        // Given
        testEntityFactory.createAndPersistExercise();

        testEntityFactory.createAndPersistExercise(
            ExerciseBuilder.anExercise(testEntityFactory.faker()).withActive(
                false
            )
        );

        // When
        var result = exerciseRepository.findByFilters(
            null,
            null,
            null,
            null,
            PageRequest.of(0, 20)
        );

        // Then
        assertThat(result).allMatch(Exercise::isActive);
    }
}
