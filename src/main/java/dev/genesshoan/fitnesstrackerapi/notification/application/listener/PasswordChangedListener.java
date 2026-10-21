package dev.genesshoan.fitnesstrackerapi.notification.application.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import dev.genesshoan.fitnesstrackerapi.auth.application.event.PasswordChanged;
import dev.genesshoan.fitnesstrackerapi.infrastructure.email.EmailSender;
import dev.genesshoan.fitnesstrackerapi.notification.application.EmailContextFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Event listener that sends the security notification sent after a password change.
 *
 * <p>Listens for {@link PasswordChanged} events published by the authentication
 * module and sends a templated email through the {@link EmailSender} infrastructure
 * port. The email warns the user about the change and points to the password
 * reset flow in case the change was not made by the account owner.
 *
 * <p>Because the event is published inside the transaction that updates the
 * password hash, the listener uses {@link TransactionalEventListener} with
 * {@link TransactionPhase.AFTER_COMMIT} so the notification is only sent once the
 * new password is persisted. The {@link Async} annotation keeps the SMTP round
 * trip out of the request thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordChangedListener {

    private static final String TEMPLATE = "notification/password-changed";
    private static final String SUBJECT = "Your Password Was Changed";

    private final EmailSender emailSender;
    private final TemplateEngine templateEngine;
    private final EmailContextFactory contextFactory;

    /**
     * Handles {@link PasswordChanged} events by rendering and sending the notification email.
     *
     * @param event the password change event containing the user data and the change timestamp
     *
     * @see TemplateEngine#process(String, Context)
     * @see EmailSender#send(String, String, String)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordChanged(PasswordChanged event) {
        log.debug("Handling password changed notification for user={}", event.username());

        Context context = contextFactory.create();
        context.setVariable("username", event.username());
        context.setVariable("changedAt", contextFactory.formatTimestamp(event.realizedAt()));
        context.setVariable("forgotPasswordUrl", contextFactory.frontendUrl("/forgot-password"));

        String html = templateEngine.process(TEMPLATE, context);
        emailSender.send(event.email(), SUBJECT, html);
    }
}
