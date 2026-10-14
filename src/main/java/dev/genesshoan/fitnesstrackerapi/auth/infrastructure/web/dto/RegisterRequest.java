package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import dev.genesshoan.fitnesstrackerapi.common.validation.ValidTimezone;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for user registration")
public record RegisterRequest(
        @Schema(description = "Username", example = "user_213") @NotBlank @Size(min = 3, max = 30)
        String username,

        @Schema(description = "User password (min 8 characters)", example = "StrongPass123!")
        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @Schema(description = "User email address", example = "user@mail.com") @NotBlank @Email @Size(max = 254)
        String email,

        @NotBlank @ValidTimezone @Size(max = 50) String timezone) {}
