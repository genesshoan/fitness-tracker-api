package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.genesshoan.fitnesstrackerapi.auth.application.command.RegisterCommand;
import dev.genesshoan.fitnesstrackerapi.auth.application.service.RegistrationService;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.MessageResponse;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.RegisterRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.ResendVerificationEmailRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.VerifyEmailRequest;
import dev.genesshoan.fitnesstrackerapi.ratelimit.domain.RateLimitPolicy;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.RateLimited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Handles account registration and email verification.
 *
 * <p>Provides endpoints for:
 * <ul>
 * <li>User registration</li>
 * <li>Email verification</li>
 * <li>Resending the verification email</li>
 * </ul>
 *
 * <p>All endpoints are public.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Registration", description = "User registration and email verification")
public class RegistrationController {

    private final RegistrationService registrationService;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account, encodes the password securely, and sends a verification email.",
            security = {})
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "User successfully created",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = MessageResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid request data",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class))),
                @ApiResponse(
                        responseCode = "409",
                        description = "Email or username already registered",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class))),
                @ApiResponse(
                        responseCode = "429",
                        description = "Too many registration attempts for this IP address or email",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @PostMapping("/register")
    @RateLimited(policy = RateLimitPolicy.REGISTRATION, accountKey = "#dto.email")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest dto) {
        RegisterCommand command = new RegisterCommand(dto.username(), dto.password(), dto.email(), dto.timezone());

        registrationService.register(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Please check your email to verify your account"));
    }

    @Operation(
            summary = "Verify email address",
            description = "Verifies a user's email address using the token sent by email.",
            security = {})
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "Email address successfully verified"),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid request data, or the token is invalid, expired or already used",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest dto) {
        registrationService.verifyEmail(dto.token());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Resend verification email",
            description =
                    "Sends a new verification email if the account exists and is not yet verified. The response is the"
                            + " same in every case, so it does not reveal whether an email is registered.",
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
                                        schema = @Schema(implementation = ProblemDetail.class))),
                @ApiResponse(
                        responseCode = "429",
                        description = "Too many verification email requests for this IP address or email",
                        content =
                                @Content(
                                        mediaType = "application/problem+json",
                                        schema = @Schema(implementation = ProblemDetail.class)))
            })
    @PostMapping("/resend-verification")
    @RateLimited(policy = RateLimitPolicy.EMAIL_VERIFICATION, accountKey = "#dto.email")
    public ResponseEntity<Void> resendVerificationEmail(@Valid @RequestBody ResendVerificationEmailRequest dto) {
        registrationService.resendVerificationEmail(dto.email());

        return ResponseEntity.noContent().build();
    }
}
