package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The achievements the user marks as seen.
 *
 * @param achievementIds the identifiers of the achievements to mark
 */
@Schema(description = "Achievements to mark as seen")
public record MarkAchievementsSeenRequest(
        @NotEmpty @Schema(description = "Identifiers of the achievements to mark as seen")
        List<UUID> achievementIds) {}
