package dev.genesshoan.fitnesstrackerapi.workout.dto;

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Notes update request dto")
public record NotesUpdateRequestDTO(
        @Size(max = 1000) @Schema(description = "Notes to update")
        String notes) {}
