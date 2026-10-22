package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web;

import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.ForgotPasswordRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.LoginRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.RegisterRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.ResendVerificationEmailRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.ResetPasswordRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.VerifyEmailRequest;
import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.UserBuilder;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class AuthControllerIT extends AbstractIntegrationTest {

    private static final Faker FAKER = new Faker();
    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String VERIFY_EMAIL_URL = "/api/v1/auth/verify-email";
    private static final String RESEND_VERIFICATION_URL = "/api/v1/auth/resend-verification";
    private static final String FORGOT_PASSWORD_URL = "/api/v1/auth/forgot-password";
    private static final String RESET_PASSWORD_URL = "/api/v1/auth/reset-password";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String PASSWORD_URL = "/api/v1/auth/password";

    private static final String PASSWORD = "StrongPass123!";

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String username;
    private String email;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        username = "shoan_" + suffix;
        email = "shoan_" + suffix + "@mail.com";

        testEntityFactory.createAndPersistUser(UserBuilder.aUser(FAKER)
                .withUsername(username)
                .withEmail(email)
                .withPasswordHash(passwordEncoder.encode(PASSWORD))
                .withEmailVerified(true));
    }

    @Test
    @DisplayName("Should register the user and ask to check the email")
    void register_ShouldReturn201WithMessage() throws Exception {
        // Given
        var request = new RegisterRequest(username + "_new", PASSWORD, email + ".new", "America/Sao_Paulo");

        // When
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 400 when the email is invalid")
    void register_ShouldReturn400WhenEmailIsInvalid() throws Exception {
        // Given
        var request = new RegisterRequest(username + "_new", PASSWORD, "invalid-email", "America/Sao_Paulo");

        // When
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 409 when the username already exists")
    void register_ShouldReturn409WhenUsernameExists() throws Exception {
        // Given
        var request = new RegisterRequest(username, PASSWORD, email + ".other", "America/Sao_Paulo");

        // When
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should return 400 when the verification token is unknown")
    void verifyEmail_ShouldReturn400WhenTokenIsUnknown() throws Exception {
        // Given
        var request = new VerifyEmailRequest("unknown-token");

        // When
        mockMvc.perform(post(VERIFY_EMAIL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 204 when resending the verification email")
    void resendVerification_ShouldReturn204() throws Exception {
        // Given
        var request = new ResendVerificationEmailRequest(email);

        // When
        mockMvc.perform(post(RESEND_VERIFICATION_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 204 when requesting a password reset")
    void forgotPassword_ShouldReturn204() throws Exception {
        // Given
        var request = new ForgotPasswordRequest(email);

        // When
        mockMvc.perform(post(FORGOT_PASSWORD_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 204 when requesting a password reset of an unknown account")
    void forgotPassword_ShouldReturn204WhenAccountIsUnknown() throws Exception {
        // Given
        var request = new ForgotPasswordRequest("nobody@mail.com");

        // When
        mockMvc.perform(post(FORGOT_PASSWORD_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 400 when the password reset token is unknown")
    void resetPassword_ShouldReturn400WhenTokenIsUnknown() throws Exception {
        // Given
        var request = new ResetPasswordRequest("unknown-token", "NewStrongPass123!");

        // When
        mockMvc.perform(post(RESET_PASSWORD_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should login and return an access token and the refresh cookie")
    void login_ShouldReturn200WithAccessTokenAndRefreshCookie() throws Exception {
        // Given
        var login = new LoginRequest(email, PASSWORD);

        // When
        var response = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn()
                .getResponse();

        // Then
        assertThat(cookieValue(response, REFRESH_COOKIE_NAME)).isNotBlank();
        assertThat(setCookieHeader(response, REFRESH_COOKIE_NAME)).contains("HttpOnly");
    }

    @Test
    @DisplayName("Should return 401 when the credentials are invalid")
    void login_ShouldReturn401WhenCredentialsAreInvalid() throws Exception {
        // Given
        var login = new LoginRequest(email, "wrong-password");

        // When
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 403 when the email is not verified")
    void login_ShouldReturn403WhenEmailIsNotVerified() throws Exception {
        // Given
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        testEntityFactory.createAndPersistUser(UserBuilder.aUser(FAKER)
                .withEmail("unverified_" + suffix + "@mail.com")
                .withPasswordHash(passwordEncoder.encode(PASSWORD))
                .withEmailVerified(false));

        var login = new LoginRequest("unverified_" + suffix + "@mail.com", PASSWORD);

        // When
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should refresh the tokens with the refresh cookie")
    void refresh_ShouldReturn200WithNewAccessToken() throws Exception {
        // Given
        var session = login();

        // When
        mockMvc.perform(post(REFRESH_URL)
                        .cookie(session.refreshCookie(), session.csrfCookie())
                        .header(CSRF_HEADER_NAME, session.csrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 403 when the refresh request has no CSRF token")
    void refresh_ShouldReturn403WhenCsrfTokenIsMissing() throws Exception {
        // Given
        var session = login();

        // When
        mockMvc.perform(post(REFRESH_URL).cookie(session.refreshCookie(), session.csrfCookie()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 when the refresh request has an invalid CSRF token")
    void refresh_ShouldReturn403WhenCsrfTokenIsInvalid() throws Exception {
        // Given
        var session = login();

        // When
        mockMvc.perform(post(REFRESH_URL)
                        .cookie(session.refreshCookie(), session.csrfCookie())
                        .header(CSRF_HEADER_NAME, "invalid-csrf-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 400 when the refresh cookie is missing")
    void refresh_ShouldReturn400WhenRefreshCookieIsMissing() throws Exception {
        // Given
        var session = login();

        // When
        mockMvc.perform(post(REFRESH_URL).cookie(session.csrfCookie()).header(CSRF_HEADER_NAME, session.csrfToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should logout and clear the refresh cookie")
    void logout_ShouldReturn204() throws Exception {
        // Given
        var session = login();

        // When
        mockMvc.perform(delete(LOGOUT_URL)
                        .cookie(session.refreshCookie(), session.csrfCookie())
                        .header(CSRF_HEADER_NAME, session.csrfToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 403 when the logout request has no CSRF token")
    void logout_ShouldReturn403WhenCsrfTokenIsMissing() throws Exception {
        // Given
        var session = login();

        // When
        mockMvc.perform(delete(LOGOUT_URL).cookie(session.refreshCookie(), session.csrfCookie()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 401 when changing the password without authentication")
    void changePassword_ShouldReturn401WhenNotAuthenticated() throws Exception {
        // When
        mockMvc.perform(patch(PASSWORD_URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    private Session login() throws Exception {
        var login = new LoginRequest(email, PASSWORD);

        var response = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        return new Session(
                cookieValue(response, REFRESH_COOKIE_NAME),
                cookieValue(response, CSRF_COOKIE_NAME),
                new Cookie(REFRESH_COOKIE_NAME, cookieValue(response, REFRESH_COOKIE_NAME)),
                new Cookie(CSRF_COOKIE_NAME, cookieValue(response, CSRF_COOKIE_NAME)));
    }

    /**
     * Reads the value of a cookie from the Set-Cookie headers of the response.
     */
    private String cookieValue(MockHttpServletResponse response, String name) {
        return response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(header -> header.startsWith(name + "="))
                .map(header -> header.substring(name.length() + 1, header.indexOf(';')))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Cookie " + name + " was not set on the response"));
    }

    /**
     * Reads the whole Set-Cookie header of a cookie, to assert on its attributes.
     */
    private String setCookieHeader(MockHttpServletResponse response, String name) {
        return response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(header -> header.startsWith(name + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Cookie " + name + " was not set on the response"));
    }

    private record Session(String refreshToken, String csrfToken, Cookie refreshCookie, Cookie csrfCookie) {}
}
