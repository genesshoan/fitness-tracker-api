package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login request payload")
public record LoginRequest(
        @Schema(
                description = "User email address",
                example = "user@mail.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Email
        String email,

        @Schema(description = "User password", example = "StrongPass123!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String password) {}
