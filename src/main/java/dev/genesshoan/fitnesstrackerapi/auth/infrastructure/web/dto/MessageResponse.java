package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generic response containing a message")
public record MessageResponse(
        @Schema(description = "Response message", example = "Please check your email to verify your account.")
        String message) {}
