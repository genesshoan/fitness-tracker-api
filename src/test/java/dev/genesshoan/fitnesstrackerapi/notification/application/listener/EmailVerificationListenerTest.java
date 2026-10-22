package dev.genesshoan.fitnesstrackerapi.notification.application.listener;

import java.time.Instant;

import dev.genesshoan.fitnesstrackerapi.auth.application.event.VerificationEmailRequested;
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
 * Unit tests for {@link EmailVerificationListener}.
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationListenerTest {

    private static final String FRONTEND_URL = "https://app.example.com";
    private static final String EMAIL = "shoan@test.com";
    private static final String USERNAME = "shoan";
    private static final String TOKEN = "raw-verification-token";
    private static final Instant EXPIRES_AT = Instant.parse("2026-01-01T12:00:00Z");

    @Mock
    private EmailSender emailSender;

    private EmailVerificationListener listener;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties("Fitness Tracker", new AppProperties.Mail(null, FRONTEND_URL));
        EmailContextFactory contextFactory = new EmailContextFactory(appProperties);

        listener = new EmailVerificationListener(emailSender, templateEngine(), contextFactory);
    }

    @Test
    @DisplayName("Should send the verification email with the rendered template")
    void handleVerificationEmailRequested_shouldSendVerificationEmail() {
        // Given
        VerificationEmailRequested event = new VerificationEmailRequested(USERNAME, EMAIL, TOKEN, EXPIRES_AT);

        // When
        listener.handleVerificationEmailRequested(event);

        // Then
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(EMAIL), eq("Verify Your Email"), html.capture());

        assertThat(html.getValue())
                .contains(USERNAME)
                .contains(FRONTEND_URL + "/verify?token=" + TOKEN)
                .contains("2026-01-01 12:00 UTC");
    }

    @Test
    @DisplayName("Should propagate email sending failures")
    void handleVerificationEmailRequested_shouldPropagateEmailFailure() {
        // Given
        VerificationEmailRequested event = new VerificationEmailRequested(USERNAME, EMAIL, TOKEN, EXPIRES_AT);
        doThrow(new EmailSendingException("Failed to send email to " + EMAIL, new RuntimeException("smtp down")))
                .when(emailSender)
                .send(anyString(), anyString(), anyString());

        // When / Then
        assertThatThrownBy(() -> listener.handleVerificationEmailRequested(event))
                .isInstanceOf(EmailSendingException.class);
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
