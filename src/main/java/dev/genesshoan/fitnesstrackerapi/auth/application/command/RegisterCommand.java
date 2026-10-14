package dev.genesshoan.fitnesstrackerapi.auth.application.command;

import java.util.Locale;

public record RegisterCommand(String username, String password, String email, String timezone) {

    public RegisterCommand {
        username = username.trim();
        email = email.trim().toLowerCase(Locale.ROOT);
    }
}
