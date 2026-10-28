package dev.genesshoan.fitnesstrackerapi.user.infrastructure.web.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User profile information response")
public record UserResponse(
        @Schema(description = "User email address", example = "user@mail.com")
        String email,

        @Schema(description = "Username", example = "user_123")
        String username,

        @Schema(description = "Account creation timestamp", example = "2026-01-31T10:15:30Z")
        Instant createdAt,

        @Schema(description = "Temporary URL for the user's profile picture; null when absent", nullable = true)
        String profilePictureUrl) {}
