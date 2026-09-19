package dev.genesshoan.fitnesstrackerapi.common.script.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExerciseSeed(
        String name,
        String slug,
        String description,
        String category,
        String difficulty,
        List<String> instructions,
        @JsonProperty("media_object_key") String mediaObjectKey,
        MuscleLinks muscles) {
    public record MuscleLinks(List<String> primary, List<String> secondary, List<String> stabilizer) {}
}
