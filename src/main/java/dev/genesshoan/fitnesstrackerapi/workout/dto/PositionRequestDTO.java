package dev.genesshoan.fitnesstrackerapi.workout.dto;

import com.sun.istack.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "Position request dto")
public record PositionRequestDTO(@NotNull @Min(1) int position) {}
