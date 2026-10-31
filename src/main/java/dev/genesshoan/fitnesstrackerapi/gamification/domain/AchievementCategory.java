package dev.genesshoan.fitnesstrackerapi.gamification.domain;

/**
 * The subject an achievement belongs to.
 */
public enum AchievementCategory {
    /**
     * Achievements awarded for reaching a count of workouts, sets, or repetitions.
     */
    MILESTONE,

    /**
     * Achievements awarded for training consistently.
     */
    CONSISTENCY,

    /**
     * Achievements awarded for accumulated training volume.
     */
    VOLUME,

    /**
     * Achievements awarded for covering a wide range of exercises.
     */
    EXPLORATION,

    /**
     * Achievements awarded for beating a personal record.
     */
    PERSONAL_RECORD
}
