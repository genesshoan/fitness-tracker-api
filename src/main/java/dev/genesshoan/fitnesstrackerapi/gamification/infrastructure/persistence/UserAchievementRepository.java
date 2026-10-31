package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.genesshoan.fitnesstrackerapi.gamification.domain.UserAchievement;

/**
 * Persistence operations for the achievements a user has reached.
 */
@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {

    /**
     * Finds the state of one achievement for one user.
     *
     * <p>The exercise is {@code null} for a global achievement.</p>
     *
     * @param userId the user the achievement belongs to
     * @param definitionId the achievement to look for
     * @param exerciseId the exercise the achievement is scoped to, or
     *        {@code null} for a global achievement
     * @return the matching state, or {@link Optional#empty()} if the user has no
     *         such achievement yet
     */
    @Query("""
            SELECT ua
            FROM UserAchievement ua
            WHERE ua.userId = :userId
                AND ua.definition.id = :definitionId
                AND ((:exerciseId IS NULL AND ua.exerciseId IS NULL) OR ua.exerciseId = :exerciseId)
            """)
    Optional<UserAchievement> findByUserIdAndDefinitionAndExercise(
            @Param("userId") UUID userId,
            @Param("definitionId") UUID definitionId,
            @Param("exerciseId") UUID exerciseId);

    /**
     * Finds the achievements a user has unlocked, most recent first.
     *
     * @param userId the user whose achievements are looked for
     * @param pageable pagination parameters
     * @return the unlocked achievements, most recent first
     */
    @EntityGraph(attributePaths = "definition")
    List<UserAchievement> findByUserIdAndUnlockedTrueOrderByUnlockedAtDesc(UUID userId, Pageable pageable);

    /**
     * Finds the achievements a user has unlocked and has not seen yet.
     *
     * @param userId the user whose achievements are looked for
     * @return the unseen unlocked achievements, most recent first
     */
    @EntityGraph(attributePaths = "definition")
    List<UserAchievement> findByUserIdAndUnlockedTrueAndSeenFalseOrderByUnlockedAtDesc(UUID userId);

    /**
     * Marks the specified achievements of a user as seen.
     *
     * @param userId the user whose achievements are marked
     * @param ids the identifiers of the achievements to mark
     * @return the number of achievements affected by the operation
     */
    @Modifying
    @Query("""
            UPDATE UserAchievement ua
            SET ua.seen = true, ua.seenAt = CURRENT_TIMESTAMP
            WHERE ua.userId = :userId AND ua.id IN :ids
            """)
    int markSeen(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    /**
     * Records the progress of a user towards one achievement, creating the row
     * when it does not exist yet.
     *
     * <p>The operation is idempotent: the same call twice leaves one row with
     * the best progress and the first unlock instant. The progress never
     * decreases, because the progress of an achievement that has been reached
     * does not go backwards.</p>
     *
     * @param id the identifier of the row to create
     * @param userId the user the achievement belongs to
     * @param definitionId the achievement the progress belongs to
     * @param exerciseId the exercise the achievement is scoped to, or
     *        {@code null} for a global achievement
     * @param progressValue the value reached towards the threshold
     * @param unlocked whether the achievement has been unlocked
     * @param unlockedAt the instant of the unlock, or {@code null} when the
     *        achievement is not unlocked
     * @param sourceSessionId the session that produced the unlock
     * @param sourceSetId the set that produced the unlock
     */
    @Modifying
    @Query(value = """
                    INSERT INTO user_achievements (
                        id, user_id, achievement_definition_id, exercise_id, progress_value,
                        unlocked, unlocked_at, source_session_id, source_set_id, seen,
                        created_at, updated_at)
                    VALUES (
                        :id, :userId, :definitionId, :exerciseId, :progressValue,
                        :unlocked, :unlockedAt, :sourceSessionId, :sourceSetId, FALSE,
                        NOW(), NOW())
                    ON CONFLICT (user_id, achievement_definition_id, exercise_id) DO UPDATE
                    SET progress_value = GREATEST(user_achievements.progress_value, EXCLUDED.progress_value),
                        unlocked = user_achievements.unlocked OR EXCLUDED.unlocked,
                        unlocked_at = COALESCE(user_achievements.unlocked_at, EXCLUDED.unlocked_at),
                        updated_at = NOW()
                    """, nativeQuery = true)
    void upsert(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("definitionId") UUID definitionId,
            @Param("exerciseId") UUID exerciseId,
            @Param("progressValue") Double progressValue,
            @Param("unlocked") boolean unlocked,
            @Param("unlockedAt") Instant unlockedAt,
            @Param("sourceSessionId") UUID sourceSessionId,
            @Param("sourceSetId") UUID sourceSetId);
}
