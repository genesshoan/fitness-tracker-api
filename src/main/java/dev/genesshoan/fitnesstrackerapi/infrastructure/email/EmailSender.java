package dev.genesshoan.fitnesstrackerapi.infrastructure.email;

import java.io.UnsupportedEncodingException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.EmailSendingException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    /**
     * Sends an HTML email to a recipient.
     *
     * @param recipient the address the email is sent to
     * @param subject the email subject
     * @param html the HTML body
     * @throws EmailSendingException if the message cannot be built or delivered
     */
    public void send(String recipient, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(emailProperties.from(), emailProperties.fromName());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            throw new EmailSendingException("Failed to send email to " + recipient, e);
        }
    }
}
