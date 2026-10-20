package dev.genesshoan.fitnesstrackerapi.user.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for username change")
public record ChangeUsernameRequest(
        @Schema(description = "New username", example = "newuser_144", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String newUsername) {}
