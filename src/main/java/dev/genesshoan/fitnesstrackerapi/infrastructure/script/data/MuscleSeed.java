package dev.genesshoan.fitnesstrackerapi.infrastructure.script.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MuscleSeed(
        String name,
        String slug,
        String region,
        @JsonProperty("assets") List<MuscleAssetSeed> assets) {}
