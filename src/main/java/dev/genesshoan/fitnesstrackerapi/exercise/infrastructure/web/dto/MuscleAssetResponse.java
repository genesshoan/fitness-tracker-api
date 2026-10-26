package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Muscle asset response DTO")
public record MuscleAssetResponse(UUID id, String objectKey, String variant, String view) {}
