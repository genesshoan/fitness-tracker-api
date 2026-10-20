package dev.genesshoan.fitnesstrackerapi.user.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response returned after a profile picture upload or replacement.
 *
 * @param url temporary URL for the processed profile picture
 */
@Schema(description = "Profile picture access response")
public record ProfilePictureResponse(String url) {}
