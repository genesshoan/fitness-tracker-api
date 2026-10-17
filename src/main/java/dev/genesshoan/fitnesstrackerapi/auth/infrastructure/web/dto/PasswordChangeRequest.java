package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for password change")
public record PasswordChangeRequest(
        @Schema(description = "Old user password", example = "12345678", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(min = 8, max = 100)
        String oldPassword,

        @Schema(description = "New user password", example = "12345678", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(min = 8, max = 100)
        String newPassword) {}
