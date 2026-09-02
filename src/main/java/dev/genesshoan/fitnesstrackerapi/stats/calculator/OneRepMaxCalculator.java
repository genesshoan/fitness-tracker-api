package dev.genesshoan.fitnesstrackerapi.stats.calculator;

public class OneRepMaxCalculator {

    public static double calculate(int reps, double weightLifted) {

        return weightLifted * (1 + reps / 30.0);
    }
}
