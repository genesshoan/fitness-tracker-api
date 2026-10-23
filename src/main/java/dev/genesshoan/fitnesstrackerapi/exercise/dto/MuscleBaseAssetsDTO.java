package dev.genesshoan.fitnesstrackerapi.exercise.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Base assets used for the muscle intensity visualization")
public record MuscleBaseAssetsDTO(
        @Schema(
                description = "Front view base asset URL",
                example = "https://cdn.example.com/muscles/male/front/male_front_base.png")
        String frontBaseAssetUrl,

        @Schema(
                description = "Back view base asset URL",
                example = "https://cdn.example.com/muscles/male/back/male_back_base.png")
        String backBaseAssetUrl) {}
