package dev.genesshoan.fitnesstrackerapi.stats.domain;

import java.util.UUID;

/** A newly achieved personal record and the record it surpassed, if any. */
public record Achievement(
        AchievementType type, Double value, Double previousValue, UUID previousSetId, UUID previousSessionId) {}
