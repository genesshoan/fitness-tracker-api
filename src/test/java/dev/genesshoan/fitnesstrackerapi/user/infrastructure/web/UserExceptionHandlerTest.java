package dev.genesshoan.fitnesstrackerapi.user.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import dev.genesshoan.fitnesstrackerapi.user.application.exception.InvalidProfilePictureException;
import dev.genesshoan.fitnesstrackerapi.user.application.exception.ProfilePictureProcessingException;
import dev.genesshoan.fitnesstrackerapi.user.application.exception.UserRegistrationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserExceptionHandlerTest {

    private final UserExceptionHandler handler = new UserExceptionHandler();
    private final HttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/user/me/profile-picture");

    @Test
    @DisplayName("Should not expose the internal registration failure message")
    void handleUserRegistration_ShouldNotExposeTheInternalFailureMessage() {
        var response = handler.handleUserRegistration(
                new UserRegistrationException(
                        "Unable to register user: jdbc://db.internal",
                        new IllegalStateException("constraint uk_user_email")),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getTitle()).isEqualTo("User registration error");
        assertThat(response.getBody().getDetail()).isEqualTo("The user could not be registered");
        assertThat(response.getBody().getDetail()).doesNotContain("db.internal", "constraint");
    }

    @Test
    @DisplayName("Should return 400 with the rejection reason for an invalid profile picture")
    void handleInvalidProfilePicture_ShouldReturn400WithTheRejectionReason() {
        var response = handler.handleInvalidProfilePicture(
                new InvalidProfilePictureException("Unsupported file type: text/plain"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getTitle()).isEqualTo("Invalid profile picture");
        assertThat(response.getBody().getDetail()).isEqualTo("Unsupported file type: text/plain");
    }

    @Test
    @DisplayName("Should not expose internal processing message")
    void handleProfilePictureProcessing_ShouldNotExposeInternalProcessingMessage() {
        var response = handler.handleProfilePictureProcessing(
                new ProfilePictureProcessingException(
                        "processing failed: /tmp/private-image", new IllegalStateException("codec internals")),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getTitle()).isEqualTo("Image processing error");
        assertThat(response.getBody().getDetail()).isEqualTo("The profile picture could not be processed");
        assertThat(response.getBody().getDetail()).doesNotContain("private-image", "codec");
    }
}
