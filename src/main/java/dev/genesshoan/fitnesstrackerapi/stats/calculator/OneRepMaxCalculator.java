package dev.genesshoan.fitnesstrackerapi.stats.calculator;

public class OneRepMaxCalculator {

    /** Estimates one-repetition maximum using {@code weight * (1 + reps / 30)}. */
    public static double calculate(int reps, double weightLifted) {

        return weightLifted * (1 + reps / 30.0);
    }
}
