package dev.genesshoan.fitnesstrackerapi.auth.application.event;

import java.time.Instant;

public record VerificationEmailRequested(String username, String email, String token, Instant expiresAt) {}
