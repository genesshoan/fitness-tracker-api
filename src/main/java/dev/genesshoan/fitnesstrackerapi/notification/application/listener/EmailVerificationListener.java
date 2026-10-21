package dev.genesshoan.fitnesstrackerapi.notification.application.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import dev.genesshoan.fitnesstrackerapi.auth.application.event.VerificationEmailRequested;
import dev.genesshoan.fitnesstrackerapi.infrastructure.email.EmailSender;
import dev.genesshoan.fitnesstrackerapi.notification.application.EmailContextFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Event listener that handles verification email notifications asynchronously.
 *
 * <p>Listens for {@link VerificationEmailRequested} events published by the
 * authentication service and sends templated verification emails through
 * the {@link EmailSender} infrastructure port.
 *
 * <p>This listener uses transactional event handling with
 * {@link TransactionalEventListener}, ensuring the email is only sent
 * after the database transaction is successfully committed. The
 * {@link Async} annotation ensures the email sending operation does not
 * block the main application thread.
 *
 * <p>Uses Thymeleaf templates from {@code templates/notification/} to render
 * HTML email content with the verification link and expiration information
 * provided by the event.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationListener {

    private static final String TEMPLATE = "notification/verification-email";
    private static final String SUBJECT = "Verify Your Email";

    private final EmailSender emailSender;
    private final TemplateEngine templateEngine;
    private final EmailContextFactory contextFactory;

    /**
     * Handles {@link VerificationEmailRequested} events by rendering and sending emails.
     *
     * <p>This method is invoked asynchronously after the transaction commits,
     * processes the event data into an HTML email using Thymeleaf template
     * rendering, and sends it via the {@link EmailSender}.
     *
     * @param event the verification email request containing user data, token, and expiration
     *
     * @see TemplateEngine#process(String, Context)
     * @see EmailSender#send(String, String, String)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVerificationEmailRequested(VerificationEmailRequested event) {
        log.debug("Handling verification email request for user={}", event.username());

        Context context = contextFactory.create();
        context.setVariable("username", event.username());
        context.setVariable("verificationUrl", contextFactory.frontendUrl("/verify?token=" + event.token()));
        context.setVariable("expiresAt", contextFactory.formatTimestamp(event.expiresAt()));

        String html = templateEngine.process(TEMPLATE, context);
        emailSender.send(event.email(), SUBJECT, html);
    }
}
