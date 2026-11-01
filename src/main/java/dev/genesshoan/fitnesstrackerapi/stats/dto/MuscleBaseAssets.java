package dev.genesshoan.fitnesstrackerapi.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Base assets used for the muscle intensity visualization.
 *
 * @param frontBaseAssetUrl the public URL of the front view base asset
 * @param backBaseAssetUrl  the public URL of the back view base asset
 */
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
