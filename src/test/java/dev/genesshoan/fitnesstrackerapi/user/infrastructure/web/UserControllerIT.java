package dev.genesshoan.fitnesstrackerapi.user.infrastructure.web;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import dev.genesshoan.fitnesstrackerapi.common.storage.ObjectStoragePort;
import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("Integration Tests - User Controller")
class UserControllerIT extends AbstractIntegrationTest {

    @MockitoBean
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
                        .file(new MockMultipartFile("file", "avatar.png", MediaType.IMAGE_PNG_VALUE, validPng()))
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
                        .file(new MockMultipartFile(
                                "file", "avatar.txt", MediaType.TEXT_PLAIN_VALUE, "not-an-image".getBytes()))
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
                        .file(new MockMultipartFile("file", "avatar.png", MediaType.IMAGE_PNG_VALUE, validPng()))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return the profile of the authenticated user")
    void getProfile_ShouldReturn200WithTheAuthenticatedUserProfile() throws Exception {
        mockMvc.perform(get("/api/v1/user/me").with(user(asUser(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.profilePictureUrl").doesNotExist());
    }

    @Test
    @DisplayName("Should require authentication to read the profile")
    void getProfile_ShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/user/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should change the username of the authenticated user")
    void changeUsername_ShouldReturn204AndPersistTheNewUsername() throws Exception {
        String newUsername = "renamed_" + user.getId().toString().substring(0, 8);

        mockMvc.perform(put("/api/v1/user/me/username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("newUsername", newUsername)))
                        .with(user(asUser(user))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/user/me").with(user(asUser(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(newUsername));
    }

    @Test
    @DisplayName("Should reject a username change to the current username")
    void changeUsername_ShouldReturn400WhenNewUsernameMatchesCurrentOne() throws Exception {
        mockMvc.perform(put("/api/v1/user/me/username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("newUsername", user.getUsername())))
                        .with(user(asUser(user))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("New username must be different from current one"));
    }

    @Test
    @DisplayName("Should reject a blank username")
    void changeUsername_ShouldReturn400WhenNewUsernameIsBlank() throws Exception {
        mockMvc.perform(put("/api/v1/user/me/username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("newUsername", " ")))
                        .with(user(asUser(user))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should require authentication to change the username")
    void changeUsername_ShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(put("/api/v1/user/me/username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("newUsername", "someone"))))
                .andExpect(status().isUnauthorized());
    }

    private UserDetails asUser(User target) {
        return new UserDetailsImpl(
                target.getId(),
                target.getEmail(),
                target.getEmail(),
                target.getUsername(),
                target.getPasswordHash(),
                target.isEmailVerified(),
                List.of(new SimpleGrantedAuthority("ROLE_" + target.getRole().name())));
    }

    private byte[] validPng() throws Exception {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }
}
