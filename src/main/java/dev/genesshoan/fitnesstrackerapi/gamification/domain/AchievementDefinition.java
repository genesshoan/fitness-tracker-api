package dev.genesshoan.fitnesstrackerapi.gamification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import dev.genesshoan.fitnesstrackerapi.common.domain.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A badge the user can earn.
 *
 * <p>The definition is the catalog entry. It describes what has to happen for
 * the badge to be unlocked and never changes once the user has earned it.</p>
 *
 * <p>There are two kinds of rule. A {@link RuleType#THRESHOLD} rule is
 * expressed entirely as data, so a new badge of that kind is added by inserting
 * a row. A {@link RuleType#PERSONAL_RECORD} rule is evaluated in code, because
 * comparing against the previous best needs semantics that a threshold cannot
 * express.</p>
 */
@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Table(name = "achievement_definitions")
public class AchievementDefinition extends BaseEntity {

    /**
     * Stable machine key of the achievement.
     */
    @Column(nullable = false, updatable = false, length = 64, unique = true)
    private String code;

    /**
     * Display name of the achievement.
     */
    @Column(nullable = false, length = 128)
    private String name;

    /**
     * Description of what the user has to do to earn the achievement.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The subject the achievement belongs to.
     */
    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private AchievementCategory category;

    /**
     * The visual rarity of the achievement.
     */
    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private AchievementTier tier;

    /**
     * The kind of rule that unlocks the achievement.
     */
    @Column(nullable = false, length = 24)
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    /**
     * The metric the achievement is measured against.
     */
    @Column(nullable = false, length = 48)
    @Enumerated(EnumType.STRING)
    private AchievementMetric metric;

    /**
     * The comparison applied between the metric value and the threshold.
     */
    @Column(nullable = false, length = 8)
    @Enumerated(EnumType.STRING)
    private ComparisonOperator operator;

    /**
     * The value the metric has to reach. Only set for threshold rules.
     */
    @Column
    private Double threshold;

    /**
     * How many badges this rule produces for a single user.
     */
    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private AchievementScope scope;

    /**
     * Key of the image that represents the achievement.
     */
    @Column(length = 128)
    private String iconKey;

    /**
     * Position of the achievement in the catalog.
     */
    @Column(nullable = false)
    private int sortOrder;

    /**
     * Whether the achievement is offered to the users.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Checks that the rule of this definition is coherent.
     *
     * <p>The same rules are enforced by the database, so a definition that does
     * not pass this check would be rejected on write. The check exists to fail
     * with a meaningful message instead of a constraint violation.</p>
     *
     * @throws IllegalArgumentException when the rule is not coherent
     */
    public void validate() {
        if (ruleType == null || metric == null || scope == null) {
            throw new IllegalArgumentException("The rule of an achievement requires a type, a metric, and a scope.");
        }
        if (ruleType == RuleType.THRESHOLD) {
            if (threshold == null) {
                throw new IllegalArgumentException(
                        "A threshold achievement requires a threshold, but " + code + " has none.");
            }
            if (!metric.isThresholdMetric()) {
                throw new IllegalArgumentException(
                        "The metric " + metric + " cannot be used with a threshold rule in " + code + ".");
            }
            if (scope != AchievementScope.GLOBAL) {
                throw new IllegalArgumentException(
                        "A threshold achievement cannot be scoped to an exercise, but " + code + " is.");
            }
            return;
        }
        if (threshold != null) {
            throw new IllegalArgumentException(
                    "A personal record achievement cannot have a threshold, but " + code + " has one.");
        }
        if (metric.isThresholdMetric()) {
            throw new IllegalArgumentException(
                    "The metric " + metric + " cannot be used with a personal record rule in " + code + ".");
        }
    }
}
