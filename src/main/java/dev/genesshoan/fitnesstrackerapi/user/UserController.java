package dev.genesshoan.fitnesstrackerapi.user;

import jakarta.validation.Valid;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.user.dto.ChangePasswordRequestDTO;
import dev.genesshoan.fitnesstrackerapi.user.dto.ChangeUsernameRequestDTO;
import dev.genesshoan.fitnesstrackerapi.user.dto.ProfilePictureResponseDTO;
import dev.genesshoan.fitnesstrackerapi.user.dto.UserResponseDTO;
import dev.genesshoan.fitnesstrackerapi.user.profilepicture.usecase.ProfilePictureUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
@Tag(name = "Users", description = "Operations related to user profile management")
public class UserController {

    private final UserService userService;
    private final ProfilePictureUseCase profilePictureUseCase;

    @Operation(
            summary = "Returns the user's profile",
            description = "Returns the profile information of the authenticated user.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "User profile retrieved successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = UserResponseDTO.class))),
        @ApiResponse(
                responseCode = "401",
                description = "User is not authenticated",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "User not found",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getProfile(@AuthenticationPrincipal UserDetailsImpl principal) {

        return ResponseEntity.ok(userService.getProfile(principal.getId()));
    }

    @Operation(summary = "Changes the user's password", description = "Changes the password of the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User's password changed successfully"),
        @ApiResponse(
                responseCode = "401",
                description = "The old password does not matches the current user's password",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "User not found",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "400",
                description = "The new password is the same as the current one",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
    })
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetailsImpl principal, @Valid @RequestBody ChangePasswordRequestDTO dto) {

        userService.changePassword(principal.getId(), dto);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Changes the username", description = "Changes the username of the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User's username changed successfully"),
        @ApiResponse(
                responseCode = "400",
                description = "The new username is the same as the current one",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "User not found",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/me/username")
    public ResponseEntity<Void> changeUsername(
            @AuthenticationPrincipal UserDetailsImpl principal, @Valid @RequestBody ChangeUsernameRequestDTO dto) {

        userService.changeUsername(principal.getId(), dto);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Uploads or replaces the user's profile picture",
            description = "The same operation uploads the first picture or replaces the existing one.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Profile picture uploaded or replaced successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ProfilePictureResponseDTO.class))),
        @ApiResponse(
                responseCode = "400",
                description = "The uploaded file is invalid or the multipart part is missing",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "401",
                description = "User is not authenticated",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "User not found",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "500",
                description = "Profile picture processing or storage failed",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping(value = "/me/profile-picture", consumes = "multipart/form-data")
    public ResponseEntity<ProfilePictureResponseDTO> uploadProfilePicture(
            @AuthenticationPrincipal UserDetailsImpl principal, @RequestPart("file") MultipartFile file) {

        String url = profilePictureUseCase.uploadProfilePicture(principal.getId(), file);
        return ResponseEntity.ok(new ProfilePictureResponseDTO(url));
    }
}
