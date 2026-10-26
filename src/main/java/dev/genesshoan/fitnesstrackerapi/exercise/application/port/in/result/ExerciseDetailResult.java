package dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result;

import java.util.List;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;

/**
 * The details of an exercise, including its muscle associations.
 *
 * @param id            the exercise id
 * @param name          the exercise display name
 * @param slug          the URL-friendly exercise identifier
 * @param description   the human-readable exercise description
 * @param instructions  the ordered steps to perform the exercise
 * @param category      the exercise category
 * @param difficulty    the exercise difficulty
 * @param exerciseMuscles the muscles involved in the exercise and their impact level
 * @param gifUrl        the public URL of the exercise media, or {@code null} when there is none
 */
public record ExerciseDetailResult(
        UUID id,
        String name,
        String slug,
        String description,
        List<String> instructions,
        Category category,
        Difficulty difficulty,
        List<ExerciseMuscleResult> exerciseMuscles,
        String gifUrl) {}
