package dev.genesshoan.fitnesstrackerapi.gamification.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.UnlockedAchievementResult;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementCategory;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementDefinition;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementMetric;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementScope;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementTier;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.ComparisonOperator;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.RuleType;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.UserAchievement;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.AchievementDefinitionRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.TrainingSetRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.UserAchievementRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.projection.TrainingSetProjection;
import dev.genesshoan.fitnesstrackerapi.stats.application.port.in.TrainingMetricsUseCase;
import dev.genesshoan.fitnesstrackerapi.stats.application.port.in.result.TrainingSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementEvaluationServiceTest {

    @Mock
    private AchievementDefinitionRepository achievementDefinitionRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @Mock
    private TrainingSetRepository trainingSetRepository;

    @Mock
    private TrainingMetricsUseCase trainingMetricsUseCase;

    @Mock
    private TrainingMetricMapper trainingMetricMapper;

    @Mock
    private PersonalRecordEvaluator personalRecordEvaluator;

    @Mock
    private AchievementStateLoader achievementStateLoader;

    @InjectMocks
    private AchievementEvaluationService achievementEvaluationService;

    @Test
    @DisplayName("Should unlock a threshold achievement that is reached")
    void onSessionCompleted_ShouldUnlockAReachedThreshold() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AchievementDefinition definition = thresholdDefinition("WORKOUTS_10", 10.0);

        when(achievementDefinitionRepository.findByRuleTypeAndActiveTrue(RuleType.THRESHOLD))
                .thenReturn(List.of(definition));
        when(trainingMetricsUseCase.getSnapshot(userId)).thenReturn(snapshot(12, 0));
        when(trainingMetricMapper.valueOf(eq(AchievementMetric.TOTAL_WORKOUTS), any()))
                .thenReturn(12.0);
        when(achievementStateLoader.load(eq(userId), any())).thenReturn(Map.of());
        when(trainingSetRepository.findCompletedSetsOfSession(sessionId)).thenReturn(List.of());
        when(personalRecordEvaluator.evaluateSets(userId, List.of())).thenReturn(List.of());

        // When
        List<UnlockedAchievementResult> unlocked = achievementEvaluationService.onSessionCompleted(userId, sessionId);

        // Then
        assertThat(unlocked).extracting(UnlockedAchievementResult::code).containsExactly("WORKOUTS_10");
        verify(userAchievementRepository)
                .upsert(
                        any(UUID.class),
                        eq(userId),
                        eq(definition.getId()),
                        isNull(),
                        eq(12.0),
                        eq(true),
                        any(Instant.class),
                        isNull(),
                        isNull());
    }

    @Test
    @DisplayName("Should not unlock a threshold achievement that is already unlocked")
    void onSessionCompleted_ShouldNotUnlockAnAchievementTwice() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID definitionId = UUID.randomUUID();
        AchievementDefinition definition = thresholdDefinition("WORKOUTS_10", 10.0);
        definition.setId(definitionId);

        UserAchievement alreadyUnlocked = UserAchievement.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .definition(definition)
                .progressValue(10.0)
                .unlocked(true)
                .unlockedAt(Instant.parse("2026-09-01T10:00:00Z"))
                .build();

        when(achievementDefinitionRepository.findByRuleTypeAndActiveTrue(RuleType.THRESHOLD))
                .thenReturn(List.of(definition));
        when(trainingMetricsUseCase.getSnapshot(userId)).thenReturn(snapshot(12, 0));
        when(trainingMetricMapper.valueOf(eq(AchievementMetric.TOTAL_WORKOUTS), any()))
                .thenReturn(12.0);
        when(achievementStateLoader.load(eq(userId), any()))
                .thenReturn(Map.of(new AchievementKey(definitionId, null), alreadyUnlocked));
        when(trainingSetRepository.findCompletedSetsOfSession(sessionId)).thenReturn(List.of());
        when(personalRecordEvaluator.evaluateSets(userId, List.of())).thenReturn(List.of());

        // When
        List<UnlockedAchievementResult> unlocked = achievementEvaluationService.onSessionCompleted(userId, sessionId);

        // Then
        assertThat(unlocked).isEmpty();
        verify(userAchievementRepository)
                .upsert(
                        eq(alreadyUnlocked.getId()),
                        eq(userId),
                        eq(definitionId),
                        isNull(),
                        eq(12.0),
                        eq(true),
                        eq(alreadyUnlocked.getUnlockedAt()),
                        isNull(),
                        isNull());
    }

    @Test
    @DisplayName("Should record the progress of a threshold achievement that is not reached yet")
    void onSessionCompleted_ShouldRecordTheProgressWithoutUnlocking() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AchievementDefinition definition = thresholdDefinition("WORKOUTS_10", 10.0);

        when(achievementDefinitionRepository.findByRuleTypeAndActiveTrue(RuleType.THRESHOLD))
                .thenReturn(List.of(definition));
        when(trainingMetricsUseCase.getSnapshot(userId)).thenReturn(snapshot(5, 0));
        when(trainingMetricMapper.valueOf(eq(AchievementMetric.TOTAL_WORKOUTS), any()))
                .thenReturn(5.0);
        when(achievementStateLoader.load(eq(userId), any())).thenReturn(Map.of());
        when(trainingSetRepository.findCompletedSetsOfSession(sessionId)).thenReturn(List.of());
        when(personalRecordEvaluator.evaluateSets(userId, List.of())).thenReturn(List.of());

        // When
        List<UnlockedAchievementResult> unlocked = achievementEvaluationService.onSessionCompleted(userId, sessionId);

        // Then
        assertThat(unlocked).isEmpty();
        verify(userAchievementRepository)
                .upsert(
                        any(UUID.class),
                        eq(userId),
                        eq(definition.getId()),
                        isNull(),
                        eq(5.0),
                        eq(false),
                        isNull(),
                        isNull(),
                        isNull());
    }

    @Test
    @DisplayName("Should not write anything when the progress did not change")
    void onSessionCompleted_ShouldSkipTheWriteWhenNothingChanged() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID definitionId = UUID.randomUUID();
        AchievementDefinition definition = thresholdDefinition("WORKOUTS_100", 100.0);
        definition.setId(definitionId);

        UserAchievement existing = UserAchievement.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .definition(definition)
                .progressValue(12.0)
                .unlocked(false)
                .build();

        when(achievementDefinitionRepository.findByRuleTypeAndActiveTrue(RuleType.THRESHOLD))
                .thenReturn(List.of(definition));
        when(trainingMetricsUseCase.getSnapshot(userId)).thenReturn(snapshot(12, 0));
        when(trainingMetricMapper.valueOf(eq(AchievementMetric.TOTAL_WORKOUTS), any()))
                .thenReturn(12.0);
        when(achievementStateLoader.load(eq(userId), any()))
                .thenReturn(Map.of(new AchievementKey(definitionId, null), existing));
        when(trainingSetRepository.findCompletedSetsOfSession(sessionId)).thenReturn(List.of());
        when(personalRecordEvaluator.evaluateSets(userId, List.of())).thenReturn(List.of());

        // When
        achievementEvaluationService.onSessionCompleted(userId, sessionId);

        // Then
        verify(userAchievementRepository, never())
                .upsert(any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any());
    }

    @Test
    @DisplayName("Should evaluate the personal records of the completed session")
    void onSessionCompleted_ShouldEvaluateTheRecordsOfTheSession() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        TrainingSetProjection set = new TrainingSetProjection(
                UUID.randomUUID(), UUID.randomUUID(), sessionId, 10, 100.0, null, null, Instant.now());

        when(achievementDefinitionRepository.findByRuleTypeAndActiveTrue(RuleType.THRESHOLD))
                .thenReturn(List.of());
        when(trainingSetRepository.findCompletedSetsOfSession(sessionId)).thenReturn(List.of(set));
        when(personalRecordEvaluator.evaluateSets(userId, List.of(set))).thenReturn(List.of());

        // When
        achievementEvaluationService.onSessionCompleted(userId, sessionId);

        // Then
        verify(personalRecordEvaluator).evaluateSets(userId, List.of(set));
        verifyNoInteractions(trainingMetricsUseCase);
    }

    @Test
    @DisplayName("Should evaluate only the set that was completed")
    void onSetCompleted_ShouldEvaluateOnlyTheGivenSet() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID setId = UUID.randomUUID();
        TrainingSetProjection set =
                new TrainingSetProjection(setId, UUID.randomUUID(), sessionId, 10, 100.0, null, null, Instant.now());

        when(trainingSetRepository.findCompletedSet(setId, userId)).thenReturn(java.util.Optional.of(set));
        when(personalRecordEvaluator.evaluateSets(userId, List.of(set))).thenReturn(List.of());

        // When
        achievementEvaluationService.onSetCompleted(userId, sessionId, setId);

        // Then
        verify(personalRecordEvaluator).evaluateSets(userId, List.of(set));
    }

    @Test
    @DisplayName("Should evaluate nothing when the set does not belong to the user")
    void onSetCompleted_ShouldEvaluateNothingWhenTheSetIsNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID setId = UUID.randomUUID();

        when(trainingSetRepository.findCompletedSet(setId, userId)).thenReturn(java.util.Optional.empty());

        // When
        List<UnlockedAchievementResult> unlocked =
                achievementEvaluationService.onSetCompleted(userId, sessionId, setId);

        // Then
        assertThat(unlocked).isEmpty();
        verifyNoInteractions(personalRecordEvaluator);
    }

    @Test
    @DisplayName("Should delegate the recalculation of the records of an exercise")
    void refreshExerciseRecords_ShouldDelegateToTheRecordEvaluator() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        // When
        achievementEvaluationService.refreshExerciseRecords(userId, exerciseId);

        // Then
        verify(personalRecordEvaluator).refreshExerciseRecords(userId, exerciseId);
    }

    private AchievementDefinition thresholdDefinition(String code, double threshold) {
        AchievementDefinition definition = AchievementDefinition.builder()
                .code(code)
                .name(code)
                .category(AchievementCategory.MILESTONE)
                .tier(AchievementTier.BRONZE)
                .ruleType(RuleType.THRESHOLD)
                .metric(AchievementMetric.TOTAL_WORKOUTS)
                .operator(ComparisonOperator.GTE)
                .threshold(threshold)
                .scope(AchievementScope.GLOBAL)
                .build();
        definition.setId(UUID.randomUUID());
        return definition;
    }

    private TrainingSnapshot snapshot(long totalWorkouts, int longestStreak) {
        return new TrainingSnapshot(totalWorkouts, 0, 0, 0.0, 0, 0.0, 0, longestStreak);
    }
}
