package dev.genesshoan.fitnesstrackerapi.testdata.builder;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ExerciseMuscle;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.datafaker.Faker;

public class ExerciseBuilder {

    private final Faker faker;
    private String name;
    private String slug;
    private String description;
    private Category category = Category.STRENGTH;
    private Difficulty difficulty = Difficulty.INTERMEDIATE;
    private boolean active = true;
    private Set<ExerciseMuscle> exerciseMuscles = new HashSet<>();

    public ExerciseBuilder(Faker faker) {
        this.faker = faker;
        this.name = faker.ancient().hero() + UUID.randomUUID().toString();
        this.slug = faker.internet().slug() + UUID.randomUUID().toString();
        this.description = faker.lorem().sentence();
    }

    public static ExerciseBuilder anExercise(Faker faker) {
        return new ExerciseBuilder(faker);
    }

    public ExerciseBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ExerciseBuilder withSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public ExerciseBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ExerciseBuilder withCategory(Category category) {
        this.category = category;
        return this;
    }

    public ExerciseBuilder withDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        return this;
    }

    public ExerciseBuilder withActive(boolean active) {
        this.active = active;
        return this;
    }

    public ExerciseBuilder withExerciseMuscles(
        Set<ExerciseMuscle> exerciseMuscles
    ) {
        this.exerciseMuscles = exerciseMuscles;
        return this;
    }

    public Exercise build() {
        return Exercise.builder()
            .name(name)
            .slug(slug)
            .description(description)
            .category(category)
            .difficulty(difficulty)
            .active(active)
            .exerciseMuscles(exerciseMuscles)
            .build();
    }
}
