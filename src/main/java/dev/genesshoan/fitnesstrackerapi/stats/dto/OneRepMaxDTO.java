package dev.genesshoan.fitnesstrackerapi.stats.dto;

import java.util.UUID;

public record OneRepMaxDTO(UUID exerciseId, double estimatedOneRepMax) {}
