package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.genesshoan.fitnesstrackerapi.auth.application.service.CredentialsService;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.ForgotPasswordRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.PasswordChangeRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.ResetPasswordRequest;
import dev.genesshoan.fitnesstrackerapi.common.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Handles password management for the authenticated user and the public
 * password reset flow.
 *
 * <p>Provides endpoints for:
 * <ul>
 * <li>Changing the password of the authenticated user</li>
 * <li>Requesting a password reset email</li>
 * <li>Resetting the password with a password reset token</li>
 * </ul>
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

    @Operation(
            summary = "Request a password reset email",
            description =
                    "Sends a password reset email if the account exists. The response is the same in every case, so it"
                            + " does not reveal whether an email is registered.",
            security = {})
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "Request accepted"),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid request data",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest dto) {
        credentialsService.requestPasswordReset(dto.email());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Reset the password",
            description =
                    "Resets the password using the token sent by email. All refresh tokens of the user are revoked, so"
                            + " every session must log in again.",
            security = {})
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "Password reset successfully"),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid request data, the token is invalid, expired or already used, or the new"
                                + " password equals the current one",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest dto) {
        credentialsService.resetPassword(dto.token(), dto.newPassword());

        return ResponseEntity.noContent().build();
    }
}
