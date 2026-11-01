package dev.genesshoan.fitnesstrackerapi.gamification.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.EvaluateAchievementsUseCase;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.UnlockedAchievementResult;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementDefinition;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.ComparisonOperator;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.RuleType;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.UserAchievement;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.AchievementDefinitionRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.TrainingSetRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.UserAchievementRepository;
import dev.genesshoan.fitnesstrackerapi.stats.application.port.in.TrainingMetricsUseCase;
import dev.genesshoan.fitnesstrackerapi.stats.application.port.in.result.TrainingSnapshot;
import lombok.RequiredArgsConstructor;

/**
 * Evaluates the achievements of a user when they train.
 *
 * <p>The threshold achievements are read from the catalog and compared against
 * the training totals of the user. The personal record achievements are
 * delegated to the {@link PersonalRecordEvaluator}, because comparing against
 * the previous best needs the records themselves.</p>
 *
 * <p>The evaluation runs in the transaction of the training event, so the
 * training state and the achievement state are committed together.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AchievementEvaluationService implements EvaluateAchievementsUseCase {

    private final AchievementDefinitionRepository achievementDefinitionRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final TrainingSetRepository trainingSetRepository;
    private final TrainingMetricsUseCase trainingMetricsUseCase;
    private final TrainingMetricMapper trainingMetricMapper;
    private final PersonalRecordEvaluator personalRecordEvaluator;
    private final AchievementStateLoader achievementStateLoader;

    @Override
    public List<UnlockedAchievementResult> onSessionCompleted(UUID userId, UUID sessionId) {

        List<UnlockedAchievementResult> unlocked = new ArrayList<>(evaluateThresholds(userId));

        unlocked.addAll(personalRecordEvaluator.evaluateSets(
                userId, trainingSetRepository.findCompletedSetsOfSession(sessionId)));

        return unlocked;
    }

    @Override
    public List<UnlockedAchievementResult> onSetCompleted(UUID userId, UUID sessionId, UUID setId) {

        return trainingSetRepository
                .findCompletedSet(setId, userId)
                .map(set -> personalRecordEvaluator.evaluateSets(userId, List.of(set)))
                .orElseGet(List::of);
    }

    @Override
    public void refreshExerciseRecords(UUID userId, UUID exerciseId) {
        personalRecordEvaluator.refreshExerciseRecords(userId, exerciseId);
    }

    /**
     * Evaluates the threshold achievements against the training totals of the
     * user.
     *
     * <p>The progress of an achievement never goes backwards, and an
     * achievement that was unlocked stays unlocked.</p>
     */
    private List<UnlockedAchievementResult> evaluateThresholds(UUID userId) {

        List<AchievementDefinition> definitions =
                achievementDefinitionRepository.findByRuleTypeAndActiveTrue(RuleType.THRESHOLD);

        if (definitions.isEmpty()) {
            return List.of();
        }

        TrainingSnapshot snapshot = trainingMetricsUseCase.getSnapshot(userId);
        Map<AchievementKey, UserAchievement> states = achievementStateLoader.load(userId, definitions);

        List<UnlockedAchievementResult> unlocked = new ArrayList<>();

        for (AchievementDefinition definition : definitions) {

            double value = trainingMetricMapper.valueOf(definition.getMetric(), snapshot);
            boolean reached = isReached(definition, value);

            AchievementKey key = new AchievementKey(definition.getId(), null);
            UserAchievement existing = states.get(key);
            boolean alreadyUnlocked = existing != null && existing.isUnlocked();

            if (!reached
                    && existing != null
                    && existing.getProgressValue() != null
                    && existing.getProgressValue() >= value) {
                continue;
            }

            UUID id = existing != null ? existing.getId() : UUID.randomUUID();
            Instant unlockedAt = alreadyUnlocked ? existing.getUnlockedAt() : (reached ? Instant.now() : null);

            userAchievementRepository.upsert(
                    id, userId, definition.getId(), null, value, reached, unlockedAt, null, null);

            if (reached && !alreadyUnlocked) {
                unlocked.add(new UnlockedAchievementResult(
                        id, definition.getCode(), definition.getName(), definition.getTier(), null, unlockedAt));
            }
        }

        return unlocked;
    }

    private boolean isReached(AchievementDefinition definition, double value) {
        double threshold = definition.getThreshold();
        return definition.getOperator() == ComparisonOperator.GT ? value > threshold : value >= threshold;
    }
}
