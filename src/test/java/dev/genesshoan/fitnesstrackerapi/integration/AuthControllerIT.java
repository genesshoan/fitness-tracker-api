package dev.genesshoan.fitnesstrackerapi.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.genesshoan.fitnesstrackerapi.auth.dto.LoginRequestDTO;
import dev.genesshoan.fitnesstrackerapi.auth.dto.RegisterRequestDTO;
import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AuthControllerIT extends AbstractIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";

    @Test
    @DisplayName("Should register user and return tokens")
    void register_ShouldReturn201WithTokens() throws Exception {
        var request = new RegisterRequestDTO(
            "shoan",
            "StrongPass123!",
            "shoan@mail.com"
        );

        mockMvc
            .perform(
                post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.access_token").isNotEmpty())
            .andExpect(jsonPath("$.refresh_token").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 when email is invalid")
    void register_ShouldReturn400WhenEmailIsInvalid() throws Exception {
        var request = new RegisterRequestDTO(
            "shoan",
            "StrongPass123!",
            "invalid-email"
        );

        mockMvc
            .perform(
                post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 409 when username already exists")
    void register_ShouldReturn409WhenUsernameExists() throws Exception {
        var request = new RegisterRequestDTO(
            "shoan",
            "StrongPass123!",
            "shoan@mail.com"
        );

        mockMvc
            .perform(
                post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated());

        var duplicate = new RegisterRequestDTO(
            "shoan",
            "StrongPass123!",
            "other@mail.com"
        );

        mockMvc
            .perform(
                post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicate))
            )
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should login and return tokens")
    void login_ShouldReturn200WithTokens() throws Exception {
        var register = new RegisterRequestDTO(
            "shoan",
            "StrongPass123!",
            "shoan@mail.com"
        );

        mockMvc
            .perform(
                post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(register))
            )
            .andExpect(status().isCreated());

        var login = new LoginRequestDTO("shoan@mail.com", "StrongPass123!");

        mockMvc
            .perform(
                post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(login))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").isNotEmpty())
            .andExpect(jsonPath("$.refresh_token").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 401 when credentials are invalid")
    void login_ShouldReturn401WhenCredentialsInvalid() throws Exception {
        var login = new LoginRequestDTO("nobody@mail.com", "wrongpass");

        mockMvc
            .perform(
                post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(login))
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should refresh tokens successfully")
    void refresh_ShouldReturn200WithNewTokens() throws Exception {
        var register = new RegisterRequestDTO(
            "shoan",
            "StrongPass123!",
            "shoan@mail.com"
        );

        String refreshToken = JsonPath.read(
            mockMvc
                .perform(
                    post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.refresh_token"
        );

        mockMvc
            .perform(
                post(REFRESH_URL).header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + refreshToken
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").isNotEmpty())
            .andExpect(jsonPath("$.refresh_token").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 when refresh header is missing")
    void refresh_ShouldReturn400WhenHeaderMissing() throws Exception {
        mockMvc.perform(post(REFRESH_URL)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should logout successfully")
    void logout_ShouldReturn204() throws Exception {
        var register = new RegisterRequestDTO(
            "shoan",
            "StrongPass123!",
            "shoan@mail.com"
        );

        String refreshToken = JsonPath.read(
            mockMvc
                .perform(
                    post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.refresh_token"
        );

        mockMvc
            .perform(
                delete(LOGOUT_URL).header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + refreshToken
                )
            )
            .andExpect(status().isNoContent());
    }
}
