package dev.genesshoan.fitnesstrackerapi.gamification.application.port.in;

import java.util.List;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.UnlockedAchievementResult;

/**
 * Evaluates the achievements of a user when they train.
 *
 * <p>The evaluation runs inside the transaction of the training event, so the
 * training state and the achievement state are committed together. It returns
 * the achievements that were unlocked by the event, which is what the caller
 * reports back to the user right away.</p>
 */
public interface EvaluateAchievementsUseCase {

    /**
     * Evaluates the achievements of a user after a workout session was completed.
     *
     * <p>The threshold achievements are evaluated against the training totals of
     * the user, and the personal record achievements against the sets of the
     * session.</p>
     *
     * @param userId the user the achievements belong to
     * @param sessionId the session that was completed
     * @return the achievements unlocked by the session
     */
    List<UnlockedAchievementResult> onSessionCompleted(UUID userId, UUID sessionId);

    /**
     * Evaluates the personal record achievements of a user after a set was
     * completed.
     *
     * <p>The set is compared against the records the user already holds, so the
     * user learns about a new record while the session is still in progress.</p>
     *
     * @param userId the user the achievements belong to
     * @param sessionId the session the set belongs to
     * @param setId the set that was completed
     * @return the achievements unlocked by the set
     */
    List<UnlockedAchievementResult> onSetCompleted(UUID userId, UUID sessionId, UUID setId);

    /**
     * Recalculates the personal records of a user for one exercise.
     *
     * <p>This is the repair path for the operations that invalidate a record,
     * such as editing, uncompleting, or deleting a set. The unlocked
     * achievements are kept, because the achievement already happened.</p>
     *
     * @param userId the user the records belong to
     * @param exerciseId the exercise whose records are recalculated
     */
    void refreshExerciseRecords(UUID userId, UUID exerciseId);
}
