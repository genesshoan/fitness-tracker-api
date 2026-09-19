package dev.genesshoan.fitnesstrackerapi.common.script.data;

import java.util.List;

public record ExerciseSeed(
        String name,
        String slug,
        String description,
        String category,
        String difficulty,
        List<String> instructions,
        String mediaObjectKey,
        MuscleLinks muscles) {
    public record MuscleLinks(List<String> primary, List<String> secondary, List<String> stabilizer) {}
}
