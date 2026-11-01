package dev.genesshoan.fitnesstrackerapi.progressrecord.dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Progress record response DTO")
public record ProgressRecordResponseDTO(
        @Schema(description = "Progress record id") UUID id,

        @Schema(description = "Date when the progress was recorded")
        LocalDate recordedAt,

        @Schema(description = "User weight in kilograms") Double weightKg,

        @Schema(description = "User body fat percentage") Double bodyFatPercentage) {}
