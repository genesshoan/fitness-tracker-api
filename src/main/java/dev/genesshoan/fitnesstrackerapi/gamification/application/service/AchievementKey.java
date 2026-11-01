package dev.genesshoan.fitnesstrackerapi.gamification.application.service;

import java.util.UUID;

/**
 * Identifies the state of one achievement for one scope.
 *
 * <p>A global achievement has no exercise, while an achievement scoped to an
 * exercise has one state for each exercise.</p>
 *
 * @param definitionId the achievement
 * @param exerciseId the exercise the achievement is scoped to, or {@code null}
 *        for a global achievement
 */
record AchievementKey(UUID definitionId, UUID exerciseId) {}
