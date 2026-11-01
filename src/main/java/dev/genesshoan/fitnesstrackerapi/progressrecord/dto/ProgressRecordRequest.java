package dev.genesshoan.fitnesstrackerapi.progressrecord.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request DTO for creating a progress record")
public record ProgressRecordRequestDTO(
        @Schema(description = "Date when the progress was recorded") @NotNull @PastOrPresent
        LocalDate recordedAt,

        @Schema(description = "User weight in kilograms") @NotNull @Positive @DecimalMax("500.0")
        Double weightKg,

        @Schema(description = "User body fat percentage") @DecimalMin("0.0") @DecimalMax("100.0")
        Double bodyFatPercentage) {}
