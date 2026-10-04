package dev.genesshoan.fitnesstrackerapi.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import dev.genesshoan.fitnesstrackerapi.common.storage.ObjectStoragePort;
import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;

@Transactional
@DisplayName("Integration Tests - User Controller")
class UserControllerIT extends AbstractIntegrationTest {

    @MockBean
    private ObjectStoragePort objectStorage;

    private User user;

    @BeforeEach
    void setUp() {
        user = testEntityFactory.createAndPersistUser();
    }

    @Test
    @DisplayName("Should upload a valid profile picture")
    void uploadProfilePicture_ShouldReturn200WithUrl() throws Exception {
        when(objectStorage.getPresignedUrl(any())).thenReturn("https://storage.example/profile.jpg");

        mockMvc.perform(multipart("/api/v1/user/me/profile-picture")
                        .file(new MockMultipartFile("file", "avatar.png", MediaType.IMAGE_PNG_VALUE,
                                validPng()))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(user(asUser(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://storage.example/profile.jpg"));
    }

    @Test
    @DisplayName("Should reject an invalid profile picture")
    void uploadProfilePicture_ShouldReturn400ForInvalidImage() throws Exception {
        mockMvc.perform(multipart("/api/v1/user/me/profile-picture")
                        .file(new MockMultipartFile("file", "avatar.txt", MediaType.TEXT_PLAIN_VALUE,
                                "not-an-image".getBytes()))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(user(asUser(user))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unsupported file type: text/plain"));
    }

    @Test
    @DisplayName("Should require authentication")
    void uploadProfilePicture_ShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(multipart("/api/v1/user/me/profile-picture")
                        .file(new MockMultipartFile("file", "avatar.png", MediaType.IMAGE_PNG_VALUE,
                                validPng()))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isUnauthorized());
    }

    private UserDetails asUser(User target) {
        return new UserDetailsImpl(target);
    }

    private byte[] validPng() throws Exception {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }
}
