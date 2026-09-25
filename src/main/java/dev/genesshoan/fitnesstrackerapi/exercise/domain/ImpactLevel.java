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
    PRIMARY(1.0),
    SECONDARY(0.5),
    STABILIZER(0.25);

    private final double weight;

    ImpactLevel(double weight) {
        this.weight = weight;
    }

    /**
     * Returns the application visualization weight for this impact level.
     *
     * <p>The weight is a product heuristic, not a physiological coefficient.
     */
    public double weight() {
        return weight;
    }
}
