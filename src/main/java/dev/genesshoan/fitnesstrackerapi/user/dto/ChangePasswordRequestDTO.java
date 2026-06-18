package dev.genesshoan.fitnesstrackerapi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for password change")
public record ChangePasswordRequestDTO(

    @Schema(description = "Old user password", example = "12345678", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(min = 8, max = 100)
    String oldPassword,

    @Schema(description = "New user password", example = "12345678", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(min = 8, max = 100)
    String newPassword) {
}
