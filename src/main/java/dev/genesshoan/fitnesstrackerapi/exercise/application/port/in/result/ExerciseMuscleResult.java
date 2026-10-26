package dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;

/**
 * A muscle involved in an exercise and the role it plays in it.
 *
 * @param muscle      the target muscle
 * @param impactLevel how significantly the muscle is involved in the exercise
 */
public record ExerciseMuscleResult(MuscleResult muscle, ImpactLevel impactLevel) {}
