package dev.genesshoan.fitnesstrackerapi.auth.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import dev.genesshoan.fitnesstrackerapi.auth.event.UserRegisteredEvent;
import dev.genesshoan.fitnesstrackerapi.infrastructure.email.EmailSender;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserRegisteredEmailListener {

    private final EmailSender emailSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserRegisteredEvent event) {
        emailSender.send(event.email(), "Verify your email", """
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>Welcome to Fitness Tracker!</h1>

                    <p>Thanks for creating your account.</p>

                    <p>Your verification code is:</p>

                    <h2>123456</h2>

                    <p>This code will expire in 15 minutes.</p>

                    <p>If you didn't create this account, you can ignore this email.</p>
                </body>
                </html>
                """);
    }
}
