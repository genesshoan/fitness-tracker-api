package dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.command;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;

/**
 * A muscle associated with an exercise, with its impact level.
 *
 * @param muscleSlug  slug of an existing muscle
 * @param impactLevel role of the muscle in the exercise
 */
public record ExerciseMuscleCommand(String muscleSlug, ImpactLevel impactLevel) {}
