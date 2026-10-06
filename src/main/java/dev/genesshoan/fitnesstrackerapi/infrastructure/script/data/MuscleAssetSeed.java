package dev.genesshoan.fitnesstrackerapi.infrastructure.script.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MuscleAssetSeed(
        @JsonProperty("object_key") String objectKey,
        String variant,
        String view,
        @JsonProperty("content_type") String contentType) {}
