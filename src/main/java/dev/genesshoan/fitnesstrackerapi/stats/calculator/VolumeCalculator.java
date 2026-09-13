package dev.genesshoan.fitnesstrackerapi.stats.calculator;

import java.util.List;

import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.VolumeSetProjection;

public class VolumeCalculator {

    /** Sums {@code weightKg * reps} across the supplied projected sets. */
    public static double calculate(List<VolumeSetProjection> sets) {

        return sets.stream().mapToDouble(ss -> ss.weightKg() * ss.reps()).sum();
    }
}
