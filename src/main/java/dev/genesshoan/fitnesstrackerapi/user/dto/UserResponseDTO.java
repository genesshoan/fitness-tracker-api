package dev.genesshoan.fitnesstrackerapi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User profile information response")
public record UserResponseDTO(

    @Schema(description = "User email address", example = "user@mail.com") String email,

    @Schema(description = "Username", example = "user_123") String username) {
}
