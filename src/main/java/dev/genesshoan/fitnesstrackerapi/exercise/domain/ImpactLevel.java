package dev.genesshoan.fitnesstrackerapi.exercise.domain;

/**
 * Indicates the role a muscle plays in an exercise.
 *
 * <ul>
 *   <li>PRIMARY - the primary target muscle</li>
 *   <li>SECONDARY - a supporting muscle that also works significantly</li>
 *   <li>STABILIZER - a muscle that helps stabilize the movement</li>
 * </ul>
 */
public enum ImpactLevel {
    PRIMARY,
    SECONDARY,
    STABILIZER,
}
