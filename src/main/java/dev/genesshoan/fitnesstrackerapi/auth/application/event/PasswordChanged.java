package dev.genesshoan.fitnesstrackerapi.auth.application.event;

import java.time.Instant;

public record PasswordChanged(String username, String email, Instant realizedAt) {}
