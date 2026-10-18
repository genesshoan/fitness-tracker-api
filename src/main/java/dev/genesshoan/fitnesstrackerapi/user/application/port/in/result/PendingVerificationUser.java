package dev.genesshoan.fitnesstrackerapi.user.application.port.in.result;

import java.util.UUID;

public record PendingVerificationUser(UUID id, String username) {}
