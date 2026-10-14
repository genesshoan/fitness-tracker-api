package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to verify an email address using a verification token.")
public record VerifyEmailRequest(
        @Schema(description = "Email verification token", example = "a1b2c3d4e5f6...") @NotBlank @Size(max = 100)
        String token) {}
