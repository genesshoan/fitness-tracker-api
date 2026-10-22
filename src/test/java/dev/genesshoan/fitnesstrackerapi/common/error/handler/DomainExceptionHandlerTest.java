package dev.genesshoan.fitnesstrackerapi.common.error.handler;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.EmailSendingException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ValidationException;
import dev.genesshoan.fitnesstrackerapi.user.application.exception.FileStorageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionHandlerTest {

    private final DomainExceptionHandler handler = new DomainExceptionHandler();
    private final HttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/user/me/profile-picture");

    @Test
    @DisplayName("Should return 404 with the exception message when the resource does not exist")
    void handleNotFound_ShouldReturn404WithTheExceptionMessage() {
        var response = handler.handleNotFound(new ResourceNotFoundException("Exercise not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getTitle()).isEqualTo("Resource not found");
        assertThat(response.getBody().getDetail()).isEqualTo("Exercise not found");
        assertThat(response.getBody().getProperties()).containsEntry("path", "/api/v1/user/me/profile-picture");
    }

    @Test
    @DisplayName("Should return 409 with the exception message when the resource already exists")
    void handleConflict_ShouldReturn409WithTheExceptionMessage() {
        var response = handler.handleConflict(new ResourceAlreadyExistsException("User already exists"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getTitle()).isEqualTo("Resource already exists");
        assertThat(response.getBody().getDetail()).isEqualTo("User already exists");
    }

    @Test
    @DisplayName("Should return 400 with the exception message for a bad request")
    void handleBadRequest_ShouldReturn400WithTheExceptionMessage() {
        var response = handler.handleBadRequest(new BadRequestException("Slug is required"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad request");
        assertThat(response.getBody().getDetail()).isEqualTo("Slug is required");
    }

    @Test
    @DisplayName("Should not expose storage key or provider message")
    void handleFileStorage_ShouldNotExposeStorageKeyOrProviderMessage() {
        var response = handler.handleFileStorage(
                new FileStorageException(
                        "Could not upload file: secret/profile-picture.jpg",
                        new IllegalStateException("provider credentials")),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getTitle()).isEqualTo("File storage error");
        assertThat(response.getBody().getDetail()).isEqualTo("The profile picture could not be stored");
        assertThat(response.getBody().getDetail()).doesNotContain("secret", "credentials");
    }

    @Test
    @DisplayName("Should return 400 with the field errors when the validation fails")
    void handleValidation_ShouldReturn400WithTheFieldErrors() {
        Map<String, List<String>> errors = Map.of("newUsername", List.of("must not be blank"));

        var response = handler.handleValidation(new ValidationException(errors), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getTitle()).isEqualTo("Validation error");
        assertThat(response.getBody().getDetail()).isEqualTo("One or more fields are invalid.");
        assertThat(response.getBody().getProperties()).containsEntry("errors", errors);
    }

    @Test
    @DisplayName("Should not expose the email provider failure message")
    void handleEmailSending_ShouldNotExposeTheProviderFailureMessage() {
        var response = handler.handleEmailSending(
                new EmailSendingException(
                        "SMTP rejected: smtp.secret.example", new IllegalStateException("smtp creds")),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getTitle()).isEqualTo("Email sending error");
        assertThat(response.getBody().getDetail()).isEqualTo("The email could not be sent");
        assertThat(response.getBody().getDetail()).doesNotContain("smtp", "creds");
    }
}
