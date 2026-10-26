package dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.command;

import java.util.List;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;

/**
 * Data required to create or fully replace an exercise.
 *
 * <p>The same payload is used to create an exercise and to replace the fields of
 * an existing one, because an update overwrites every field.
 *
 * @param name         the exercise display name
 * @param slug         the URL-friendly exercise identifier, unique across all exercises
 * @param description  the human-readable exercise description
 * @param instructions the ordered steps to perform the exercise; may be null
 * @param category     the exercise category
 * @param difficulty   the exercise difficulty
 * @param muscles      the muscle associations; on update they replace the current ones
 */
public record ExerciseCommand(
        String name,
        String slug,
        String description,
        List<String> instructions,
        Category category,
        Difficulty difficulty,
        List<ExerciseMuscleCommand> muscles) {}
