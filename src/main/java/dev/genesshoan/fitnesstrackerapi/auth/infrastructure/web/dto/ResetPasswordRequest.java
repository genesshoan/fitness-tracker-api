package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to reset a password using a password reset token.")
public record ResetPasswordRequest(
        @Schema(
                description = "Password reset token",
                example = "a1b2c3d4e5f6...",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String token,

        @Schema(description = "New user password", example = "12345678", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(min = 8, max = 100)
        String newPassword) {}
