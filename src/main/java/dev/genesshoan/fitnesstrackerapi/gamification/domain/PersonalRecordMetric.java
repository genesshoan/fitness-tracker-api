package dev.genesshoan.fitnesstrackerapi.gamification.domain;

/**
 * The training metric a personal record is kept for.
 *
 * <p>Every metric in this enum is "higher is better". The metrics that are
 * compared per weight, such as {@link #MAX_REPS_AT_WEIGHT}, identify the weight
 * they belong to through the weight column of the record.</p>
 */
public enum PersonalRecordMetric {
    /**
     * The heaviest weight lifted for the exercise.
     */
    MAX_WEIGHT,

    /**
     * The best estimated one repetition maximum for the exercise.
     */
    MAX_ESTIMATED_1RM,

    /**
     * The most repetitions performed at a given weight for the exercise.
     */
    MAX_REPS_AT_WEIGHT,

    /**
     * The longest distance covered for the exercise.
     */
    MAX_DISTANCE,

    /**
     * The longest duration performed for the exercise.
     */
    MAX_DURATION
}
