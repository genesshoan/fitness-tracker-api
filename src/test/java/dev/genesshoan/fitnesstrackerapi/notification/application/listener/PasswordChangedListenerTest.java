package dev.genesshoan.fitnesstrackerapi.notification.application.listener;

import java.time.Instant;

import dev.genesshoan.fitnesstrackerapi.auth.application.event.PasswordChanged;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.EmailSendingException;
import dev.genesshoan.fitnesstrackerapi.infrastructure.email.EmailSender;
import dev.genesshoan.fitnesstrackerapi.notification.application.EmailContextFactory;
import dev.genesshoan.fitnesstrackerapi.notification.infrastructure.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link PasswordChangedListener}.
 */
@ExtendWith(MockitoExtension.class)
class PasswordChangedListenerTest {

    private static final String FRONTEND_URL = "https://app.example.com";
    private static final String EMAIL = "shoan@test.com";
    private static final String USERNAME = "shoan";
    private static final Instant REALIZED_AT = Instant.parse("2026-01-01T12:00:00Z");

    @Mock
    private EmailSender emailSender;

    private PasswordChangedListener listener;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties("Fitness Tracker", new AppProperties.Mail(null, FRONTEND_URL));
        EmailContextFactory contextFactory = new EmailContextFactory(appProperties);

        listener = new PasswordChangedListener(emailSender, templateEngine(), contextFactory);
    }

    @Test
    @DisplayName("Should send the password changed notification with the rendered template")
    void handlePasswordChanged_shouldSendNotificationEmail() {
        // Given
        PasswordChanged event = new PasswordChanged(USERNAME, EMAIL, REALIZED_AT);

        // When
        listener.handlePasswordChanged(event);

        // Then
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(EMAIL), eq("Your Password Was Changed"), html.capture());

        assertThat(html.getValue())
                .contains(USERNAME)
                .contains(FRONTEND_URL + "/forgot-password")
                .contains("2026-01-01 12:00 UTC");
    }

    @Test
    @DisplayName("Should propagate email sending failures")
    void handlePasswordChanged_shouldPropagateEmailFailure() {
        // Given
        PasswordChanged event = new PasswordChanged(USERNAME, EMAIL, REALIZED_AT);
        doThrow(new EmailSendingException("Failed to send email to " + EMAIL, new RuntimeException("smtp down")))
                .when(emailSender)
                .send(anyString(), anyString(), anyString());

        // When / Then
        assertThatThrownBy(() -> listener.handlePasswordChanged(event)).isInstanceOf(EmailSendingException.class);
    }

    private TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
