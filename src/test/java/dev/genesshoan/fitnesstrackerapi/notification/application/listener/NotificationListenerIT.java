package dev.genesshoan.fitnesstrackerapi.notification.application.listener;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.ForgotPasswordRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.LoginRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.PasswordChangeRequest;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web.dto.RegisterRequest;
import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import dev.genesshoan.fitnesstrackerapi.infrastructure.email.EmailSender;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.UserBuilder;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of the notification module.
 *
 * <p>They exercise the whole chain: the authentication module publishes an
 * application event, the event is delivered after the transaction is committed
 * and the notification module sends the templated email.
 */
public class NotificationListenerIT extends AbstractIntegrationTest {

    private static final Faker FAKER = new Faker();
    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String FORGOT_PASSWORD_URL = "/api/v1/auth/forgot-password";
    private static final String PASSWORD_URL = "/api/v1/auth/password";

    private static final String PASSWORD = "StrongPass123!";

    @MockitoBean
    private EmailSender emailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String username;
    private String email;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        username = "shoan_" + suffix;
        email = "shoan_" + suffix + "@mail.com";
    }

    @Test
    @DisplayName("Should send the verification email once the registration is committed")
    void register_ShouldSendVerificationEmail() throws Exception {
        // Given
        var request = new RegisterRequest(username + "_new", PASSWORD, email + ".new", "America/Sao_Paulo");

        // When
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Then
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailSender, timeout(10_000)).send(eq(email + ".new"), eq("Verify Your Email"), html.capture());

        assertThat(html.getValue()).contains(username + "_new").contains("/verify?token=");
    }

    @Test
    @DisplayName("Should send the password reset email once the token is committed")
    void forgotPassword_ShouldSendPasswordResetEmail() throws Exception {
        // Given
        testEntityFactory.createAndPersistUser(UserBuilder.aUser(FAKER)
                .withUsername(username)
                .withEmail(email)
                .withPasswordHash(passwordEncoder.encode(PASSWORD))
                .withEmailVerified(true));

        var request = new ForgotPasswordRequest(email);

        // When
        mockMvc.perform(post(FORGOT_PASSWORD_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Then
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailSender, timeout(10_000)).send(eq(email), eq("Reset Your Password"), html.capture());

        assertThat(html.getValue()).contains(username).contains("/reset-password?token=");
    }

    @Test
    @DisplayName("Should send the password changed notification once the new password is committed")
    void changePassword_ShouldSendPasswordChangedEmail() throws Exception {
        // Given
        testEntityFactory.createAndPersistUser(UserBuilder.aUser(FAKER)
                .withUsername(username)
                .withEmail(email)
                .withPasswordHash(passwordEncoder.encode(PASSWORD))
                .withEmailVerified(true));

        String accessToken = login();

        var request = new PasswordChangeRequest(PASSWORD, "NewStrongPass123!");

        // When
        mockMvc.perform(patch(PASSWORD_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Then
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailSender, timeout(10_000)).send(eq(email), eq("Your Password Was Changed"), html.capture());

        assertThat(html.getValue()).contains(username).contains("/forgot-password");
    }

    private String login() throws Exception {
        var login = new LoginRequest(email, PASSWORD);

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }
}
