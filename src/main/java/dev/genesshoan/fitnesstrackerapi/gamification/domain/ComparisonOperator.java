package dev.genesshoan.fitnesstrackerapi.gamification.domain;

/**
 * The comparison applied between a metric value and the threshold of an
 * achievement.
 */
public enum ComparisonOperator {
    /**
     * The metric must be greater than or equal to the threshold.
     */
    GTE,

    /**
     * The metric must be strictly greater than the threshold.
     */
    GT
}
