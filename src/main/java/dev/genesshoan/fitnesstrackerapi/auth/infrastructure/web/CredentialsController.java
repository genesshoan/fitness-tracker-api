package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.genesshoan.fitnesstrackerapi.auth.application.service.CredentialsService;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.PasswordChangeRequest;
import dev.genesshoan.fitnesstrackerapi.common.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Handles credential changes for the authenticated user.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Credentials", description = "Password management")
public class CredentialsController {

    private final CredentialsService credentialsService;

    @Operation(
            summary = "Change password",
            description =
                    "Changes the authenticated user's password after verifying the current one. All refresh tokens"
                            + " of the user are revoked, so every session must log in again once its access token"
                            + " expires.")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "Password changed successfully"),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid request data, or the new password equals the current one",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated, or the current password is incorrect",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @CurrentUserId UUID userId, @Valid @RequestBody PasswordChangeRequest dto) {
        credentialsService.changePassword(userId, dto.oldPassword(), dto.newPassword());

        return ResponseEntity.noContent().build();
    }
}
