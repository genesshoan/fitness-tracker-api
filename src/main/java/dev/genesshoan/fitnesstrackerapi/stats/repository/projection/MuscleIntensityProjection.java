package dev.genesshoan.fitnesstrackerapi.stats.repository.projection;

import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.BodyRegion;

/** JDBC projection for raw muscle stimulus grouped by impact level. */
public record MuscleIntensityProjection(
        UUID muscleId, String name, String slug, BodyRegion bodyRegion, ImpactLevel impactLevel, double rawStimulus) {}
