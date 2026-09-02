package dev.genesshoan.fitnesstrackerapi.workout.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Position request dto")
public record PositionRequestDTO(@NotNull @Min(1) int position) {}
