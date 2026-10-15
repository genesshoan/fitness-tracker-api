package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT token pair response")
public record TokenResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String accessToken) {}
