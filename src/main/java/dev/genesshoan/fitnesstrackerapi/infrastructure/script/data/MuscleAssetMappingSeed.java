package dev.genesshoan.fitnesstrackerapi.infrastructure.script.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MuscleAssetMappingSeed(
        @JsonProperty("muscle_slug") String muscleSlug,
        @JsonProperty("object_key") String objectKey,
        String variant,
        String view) {}
