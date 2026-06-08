package dev.genesshoan.fitnesstrackerapi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

@Schema(description = "Request for user registration")
public record RegisterRequestDTO(

        @Schema(
                description = "Username",
                example = "user_213",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        String username,

        @Schema(
                description = "User password (min 8 characters)",
                example = "StrongPass123!",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Length(min = 8)
        String password,

        @Schema(
                description = "User email address",
                example = "user@mail.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Email
        String email
) {
}
