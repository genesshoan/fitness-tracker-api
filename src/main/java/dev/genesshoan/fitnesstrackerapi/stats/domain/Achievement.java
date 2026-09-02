package dev.genesshoan.fitnesstrackerapi.stats.domain;

import java.util.UUID;

public record Achievement(
        AchievementType type, Double value, Double previousValue, UUID previousSetId, UUID previousSessionId) {}
