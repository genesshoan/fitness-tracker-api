package dev.genesshoan.fitnesstrackerapi.gamification.domain;

/**
 * The kind of rule that unlocks an achievement.
 */
public enum RuleType {
    /**
     * Unlocks when a cumulative training metric reaches a threshold.
     *
     * <p>The metric, the operator, and the threshold are data, so a new
     * achievement of this kind is added by inserting a row.</p>
     */
    THRESHOLD,

    /**
     * Unlocks when the user beats one of their personal records.
     *
     * <p>This rule is evaluated in code, because comparing against the previous
     * best needs semantics that a threshold cannot express.</p>
     */
    PERSONAL_RECORD
}
