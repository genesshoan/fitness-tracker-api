package dev.genesshoan.fitnesstrackerapi.gamification.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import dev.genesshoan.fitnesstrackerapi.common.domain.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * The state a user has reached for one achievement.
 *
 * <p>There is one row per user, definition, and scope. A global achievement has
 * no exercise, while an achievement scoped to an exercise has one row for each
 * exercise the user has earned it on.</p>
 *
 * <p>The references to the user, the exercise, and the training that produced
 * the unlock are plain identifiers on purpose. This module does not depend on
 * the entities of the other modules.</p>
 */
@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Table(name = "user_achievements")
public class UserAchievement extends BaseEntity {

    /**
     * The user this achievement belongs to.
     */
    @Column(nullable = false, updatable = false)
    private UUID userId;

    /**
     * The achievement this state belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "achievement_definition_id", nullable = false, updatable = false)
    private AchievementDefinition definition;

    /**
     * The exercise this achievement is scoped to. {@code null} for a global
     * achievement.
     */
    @Column(updatable = false)
    private UUID exerciseId;

    /**
     * The best value reached toward the threshold.
     *
     * <p>Only threshold achievements carry a progress value. A personal record
     * achievement is either unlocked or not, and its current record is read from
     * the personal records.</p>
     */
    @Column
    private Double progressValue;

    /**
     * Whether the achievement has been unlocked.
     */
    @Column(nullable = false)
    private boolean unlocked;

    /**
     * The instant the achievement was unlocked.
     */
    @Column
    private Instant unlockedAt;

    /**
     * The workout session that produced the unlock.
     */
    @Column(updatable = false)
    private UUID sourceSessionId;

    /**
     * The set that produced the unlock.
     */
    @Column(updatable = false)
    private UUID sourceSetId;

    /**
     * Whether the user has seen the unlock.
     */
    @Column(nullable = false)
    private boolean seen;

    /**
     * The instant the unlock was seen.
     */
    @Column
    private Instant seenAt;

    /**
     * Marks the achievement as unlocked at the given instant.
     *
     * @param unlockedAt the instant of the unlock
     */
    public void unlock(Instant unlockedAt) {
        this.unlocked = true;
        this.unlockedAt = unlockedAt;
    }

    /**
     * Marks the unlock as seen at the given instant.
     *
     * @param seenAt the instant the unlock was seen
     */
    public void markSeen(Instant seenAt) {
        this.seen = true;
        this.seenAt = seenAt;
    }
}
