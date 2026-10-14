package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to resend an email verification message.")
public record ResendVerificationEmailRequest(
        @Schema(description = "Email address to verify", example = "user@example.com") @NotBlank @Email @Size(max = 254)
        String email) {}
