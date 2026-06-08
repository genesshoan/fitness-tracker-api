package dev.genesshoan.fitnesstrackerapi.auth.dto;

public record TokenResponseDTO(

        String accessToken,
        String refreshToken
) {
}

