package dev.genesshoan.fitnesstrackerapi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT token pair response")
public record TokenResponseDTO(

        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken
) {
}

