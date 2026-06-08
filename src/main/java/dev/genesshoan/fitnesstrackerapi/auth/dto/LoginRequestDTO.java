package dev.genesshoan.fitnesstrackerapi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request payload")
public record LoginRequestDTO(

        @Schema(
                description = "User email address",
                example = "user@mail.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
    @NotBlank
    @Email
    String email,

        @Schema(
                description = "User password",
                example = "StrongPass123!",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
    @NotBlank
    String password) {
}
