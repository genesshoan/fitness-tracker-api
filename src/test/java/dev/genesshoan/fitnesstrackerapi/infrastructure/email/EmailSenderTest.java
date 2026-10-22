package dev.genesshoan.fitnesstrackerapi.infrastructure.email;

import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.EmailSendingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmailSender}.
 */
@ExtendWith(MockitoExtension.class)
class EmailSenderTest {

    private static final String RECIPIENT = "shoan@test.com";
    private static final String SUBJECT = "Verify Your Email";
    private static final String HTML = "<html><body>Hello</body></html>";

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private EmailSender emailSender;

    @BeforeEach
    void setUp() {
        emailSender = new EmailSender(mailSender, new EmailProperties("noreply@test.com", "Fitness Tracker"));
    }

    @Test
    @DisplayName("Should send the rendered html message")
    void send_shouldSendMessage() {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailSender.send(RECIPIENT, SUBJECT, HTML);

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should wrap a message building failure into an email sending exception")
    void send_shouldThrowEmailSendingExceptionOnMessagingError() {
        // Given
        when(mailSender.createMimeMessage()).thenThrow(new MailParseException("invalid address"));

        // When / Then
        assertThatThrownBy(() -> emailSender.send(RECIPIENT, SUBJECT, HTML))
                .isInstanceOf(EmailSendingException.class)
                .hasMessageContaining(RECIPIENT);
    }

    @Test
    @DisplayName("Should wrap a delivery failure into an email sending exception")
    void send_shouldThrowEmailSendingExceptionOnDeliveryError() {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("smtp unreachable")).when(mailSender).send(any(MimeMessage.class));

        // When / Then
        assertThatThrownBy(() -> emailSender.send(RECIPIENT, SUBJECT, HTML))
                .isInstanceOf(EmailSendingException.class)
                .hasMessageContaining(RECIPIENT);
    }
}
