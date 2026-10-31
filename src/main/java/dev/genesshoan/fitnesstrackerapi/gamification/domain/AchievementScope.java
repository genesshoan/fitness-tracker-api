package dev.genesshoan.fitnesstrackerapi.gamification.domain;

/**
 * How many badges a rule produces for a single user.
 */
public enum AchievementScope {
    /**
     * One badge per user, no matter how many exercises the rule applies to.
     */
    GLOBAL,

    /**
     * One badge per user and exercise, so the badge is collected as the user
     * trains different exercises.
     */
    PER_EXERCISE
}
