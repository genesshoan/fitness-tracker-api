package dev.genesshoan.fitnesstrackerapi.infrastructure.script.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DatabaseSeed(
        List<ExerciseSeed> exercises,
        List<MuscleSeed> muscles,
        @JsonProperty("muscle_assets") List<MuscleAssetSeed> muscleAssets,
        @JsonProperty("muscle_asset_mappings") List<MuscleAssetMappingSeed> muscleAssetMappings) {}
