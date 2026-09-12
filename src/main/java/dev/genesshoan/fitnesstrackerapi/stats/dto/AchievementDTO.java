package dev.genesshoan.fitnesstrackerapi.stats.dto;

import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.stats.domain.AchievementType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Achievement obtained by the user during a workout session")
public record AchievementDTO(
        @Schema(description = "Type of achievement obtained", example = "NEW_MAX_WEIGHT")
        AchievementType type,

        @Schema(description = "Value achieved", example = "120.0")
        Double value,

        @Schema(description = "Previous record value, if one existed", example = "115.0", nullable = true)
        Double previousValue,

        @Schema(
                description = "ID of the set holding the previous record",
                example = "550e8400-e29b-41d4-a716-446655440000",
                nullable = true)
        UUID previousSetId,

        @Schema(
                description = "ID of the session holding the previous record",
                example = "550e8400-e29b-41d4-a716-446655440001",
                nullable = true)
        UUID previousSessionId) {}
