package dev.genesshoan.fitnesstrackerapi.unit;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.FileStorageException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ProfilePictureProcessingException;
import dev.genesshoan.fitnesstrackerapi.common.error.handler.DomainExceptionHandler;

class DomainExceptionHandlerTest {

    private final DomainExceptionHandler handler = new DomainExceptionHandler();
    private final HttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/user/me/profile-picture");

    @Test
    void handleFileStorage_ShouldNotExposeStorageKeyOrProviderMessage() {
        var response = handler.handleFileStorage(
                new FileStorageException("Could not upload file: secret/profile-picture.jpg",
                        new IllegalStateException("provider credentials")),
                request);

        assertThat(response.getBody().getDetail()).isEqualTo("The profile picture could not be stored");
        assertThat(response.getBody().getDetail()).doesNotContain("secret", "credentials");
    }

    @Test
    void handleProcessing_ShouldNotExposeInternalProcessingMessage() {
        var response = handler.handleProfilePictureProcessing(
                new ProfilePictureProcessingException("processing failed: /tmp/private-image", 
                        new IllegalStateException("codec internals")),
                request);

        assertThat(response.getBody().getDetail()).isEqualTo("The profile picture could not be processed");
        assertThat(response.getBody().getDetail()).doesNotContain("private-image", "codec");
    }
}
