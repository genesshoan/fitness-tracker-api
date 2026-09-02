package dev.genesshoan.fitnesstrackerapi.stats.calculator;

import java.util.List;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;

public class VolumenCalculator {

    public static double calculate(List<SessionSet> sets) {

        return sets.stream()
                .filter(SessionSet::isCompleted)
                .filter(ss -> ss.getSessionExercise().getExercise().getCategory() == Category.STRENGTH)
                .filter(ss -> ss.getWeightKg() != null && ss.getReps() != null)
                .mapToDouble(ss -> ss.getWeightKg() * ss.getReps())
                .sum();
    }
}
