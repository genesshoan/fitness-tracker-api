package dev.genesshoan.fitnesstrackerapi.gamification.domain;

import java.util.Optional;

/**
 * The metric an achievement is measured against.
 *
 * <p>Each metric belongs to exactly one {@link RuleType}. Threshold metrics are
 * read from the training aggregates, while personal record metrics are read
 * from the personal records kept for the user.</p>
 */
public enum AchievementMetric {
    /**
     * The number of completed workout sessions.
     */
    TOTAL_WORKOUTS(RuleType.THRESHOLD),
    /**
     * The longest streak of consecutive training days.
     */
    LONGEST_STREAK(RuleType.THRESHOLD),
    /**
     * The number of completed sets.
     */
    TOTAL_SETS(RuleType.THRESHOLD),
    /**
     * The number of performed repetitions.
     */
    TOTAL_REPS(RuleType.THRESHOLD),
    /**
     * The accumulated training volume, in kilograms lifted.
     */
    TOTAL_VOLUME_KG(RuleType.THRESHOLD),
    /**
     * The accumulated training time, in seconds.
     */
    TOTAL_TRAINING_SECONDS(RuleType.THRESHOLD),
    /**
     * The accumulated distance, in kilometers.
     */
    TOTAL_DISTANCE_KM(RuleType.THRESHOLD),
    /**
     * The number of distinct exercises trained.
     */
    DISTINCT_EXERCISES(RuleType.THRESHOLD),

    /**
     * The heaviest weight lifted for an exercise.
     */
    MAX_WEIGHT(RuleType.PERSONAL_RECORD, PersonalRecordMetric.MAX_WEIGHT),
    /**
     * The best estimated one repetition maximum for an exercise.
     */
    MAX_ESTIMATED_1RM(RuleType.PERSONAL_RECORD, PersonalRecordMetric.MAX_ESTIMATED_1RM),
    /**
     * The most repetitions performed at a given weight for an exercise.
     */
    MAX_REPS_AT_WEIGHT(RuleType.PERSONAL_RECORD, PersonalRecordMetric.MAX_REPS_AT_WEIGHT),
    /**
     * The longest distance covered for an exercise.
     */
    MAX_DISTANCE(RuleType.PERSONAL_RECORD, PersonalRecordMetric.MAX_DISTANCE),
    /**
     * The longest duration performed for an exercise.
     */
    MAX_DURATION(RuleType.PERSONAL_RECORD, PersonalRecordMetric.MAX_DURATION);

    private final RuleType ruleType;
    private final PersonalRecordMetric personalRecordMetric;

    AchievementMetric(RuleType ruleType) {
        this(ruleType, null);
    }

    AchievementMetric(RuleType ruleType, PersonalRecordMetric personalRecordMetric) {
        this.ruleType = ruleType;
        this.personalRecordMetric = personalRecordMetric;
    }

    /**
     * Returns the kind of rule this metric can be used with.
     *
     * @return the rule type of this metric
     */
    public RuleType ruleType() {
        return ruleType;
    }

    /**
     * Returns the personal record metric this metric is kept for.
     *
     * @return the matching personal record metric, or {@link Optional#empty()}
     *         when this metric is a threshold metric
     */
    public Optional<PersonalRecordMetric> personalRecordMetric() {
        return Optional.ofNullable(personalRecordMetric);
    }

    /**
     * Checks whether this metric can be used with a threshold rule.
     *
     * @return {@code true} when this metric is a threshold metric
     */
    public boolean isThresholdMetric() {
        return ruleType == RuleType.THRESHOLD;
    }
}
