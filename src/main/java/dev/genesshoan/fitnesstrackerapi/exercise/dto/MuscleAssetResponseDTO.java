package dev.genesshoan.fitnesstrackerapi.exercise.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Muscle asset response DTO")
public record MuscleAssetResponseDTO(UUID id, String objectKey, String variant, String view, String contentType) {}
