package dev.genesshoan.fitnesstrackerapi.auth.event;

import java.util.UUID;

public record UserRegisteredEvent(UUID userId, String email) {}
