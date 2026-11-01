package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.projection;

import java.time.Instant;
import java.util.UUID;

/**
 * A completed set with the exercise it belongs to and the session it was
 * performed in.
 *
 * @param setId the identifier of the set
 * @param exerciseId the exercise the set belongs to
 * @param sessionId the session the set belongs to
 * @param reps the performed repetitions
 * @param weightKg the lifted weight
 * @param durationSeconds the performed duration
 * @param distanceKm the covered distance
 * @param performedAt the instant the training happened
 */
public record TrainingSetProjection(
        UUID setId,
        UUID exerciseId,
        UUID sessionId,
        Integer reps,
        Double weightKg,
        Integer durationSeconds,
        Double distanceKm,
        Instant performedAt) {}
