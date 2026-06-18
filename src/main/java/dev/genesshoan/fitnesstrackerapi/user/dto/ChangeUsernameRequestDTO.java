package dev.genesshoan.fitnesstrackerapi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request for username change")
public record ChangeUsernameRequestDTO(

    @Schema(description = "New username", example = "newuser_144", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String newUsername) {
}
