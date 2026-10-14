package dev.genesshoan.fitnesstrackerapi.auth.application.result;

import java.time.Instant;

public record IssuedToken(String token, Instant expiresAt) {}
