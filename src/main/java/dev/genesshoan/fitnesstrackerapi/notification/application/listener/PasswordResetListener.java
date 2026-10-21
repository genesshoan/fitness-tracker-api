package dev.genesshoan.fitnesstrackerapi.notification.application.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import dev.genesshoan.fitnesstrackerapi.auth.application.event.PasswordResetRequested;
import dev.genesshoan.fitnesstrackerapi.infrastructure.email.EmailSender;
import dev.genesshoan.fitnesstrackerapi.notification.application.EmailContextFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Event listener that handles password reset email notifications asynchronously.
 *
 * <p>Listens for {@link PasswordResetRequested} events published by the
 * authentication service and sends a templated password reset email through
 * the {@link EmailSender} infrastructure port.
 *
 * <p>This listener uses transactional event handling with
 * {@link TransactionalEventListener}, ensuring the email is only sent after the
 * reset token has been persisted. The {@link Async} annotation ensures the email
 * sending operation does not block the main application thread.
 *
 * <p>Uses Thymeleaf templates from {@code templates/notification/} to render
 * HTML email content with the reset link and expiration information provided by
 * the event.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetListener {

    private static final String TEMPLATE = "notification/reset-password-email";
    private static final String SUBJECT = "Reset Your Password";

    private final EmailSender emailSender;
    private final TemplateEngine templateEngine;
    private final EmailContextFactory contextFactory;

    /**
     * Handles {@link PasswordResetRequested} events by rendering and sending emails.
     *
     * @param event the password reset request containing user data, token, and expiration
     *
     * @see TemplateEngine#process(String, Context)
     * @see EmailSender#send(String, String, String)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordResetRequested(PasswordResetRequested event) {
        log.debug("Handling password reset email request for user={}", event.username());

        Context context = contextFactory.create();
        context.setVariable("username", event.username());
        context.setVariable("resetPasswordUrl", contextFactory.frontendUrl("/reset-password?token=" + event.token()));
        context.setVariable("expiresAt", contextFactory.formatTimestamp(event.expiresAt()));

        String html = templateEngine.process(TEMPLATE, context);
        emailSender.send(event.email(), SUBJECT, html);
    }
}
