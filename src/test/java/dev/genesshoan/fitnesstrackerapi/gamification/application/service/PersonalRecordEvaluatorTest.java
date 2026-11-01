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
import dev.genesshoan.fitnesstrackerapi.gamification.domain.PersonalRecord;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.PersonalRecordMetric;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.RuleType;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.UserAchievement;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.AchievementDefinitionRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.PersonalRecordRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.TrainingSetRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.UserAchievementRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.projection.TrainingSetProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalRecordEvaluatorTest {

    private static final Instant PERFORMED_AT = Instant.parse("2026-10-01T10:00:00Z");

    @Mock
    private AchievementDefinitionRepository achievementDefinitionRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @Mock
    private PersonalRecordRepository personalRecordRepository;

    @Mock
    private TrainingSetRepository trainingSetRepository;

    @Mock
    private AchievementStateLoader achievementStateLoader;

    @InjectMocks
    private PersonalRecordEvaluator personalRecordEvaluator;

    @Test
    @DisplayName("Should unlock the record achievements and store the records when a set beats them")
    void evaluateSets_ShouldStoreTheRecordsAndUnlockTheAchievements() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        UUID setId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        AchievementDefinition global = recordDefinition("PR_FIRST_MAX_WEIGHT", AchievementScope.GLOBAL);
        AchievementDefinition perExercise =
                recordDefinition("PR_MAX_WEIGHT_PER_EXERCISE", AchievementScope.PER_EXERCISE);

        when(achievementDefinitionRepository.findByRuleTypeAndActiveTrue(RuleType.PERSONAL_RECORD))
                .thenReturn(List.of(global, perExercise));
        when(achievementStateLoader.load(eq(userId), any())).thenReturn(Map.of());
        when(personalRecordRepository.findByUserIdAndExerciseIdIn(eq(userId), any()))
                .thenReturn(List.of());

        // When
        List<UnlockedAchievementResult> unlocked = personalRecordEvaluator.evaluateSets(
                userId, List.of(weightSet(setId, exerciseId, sessionId, 100.0, 10)));

        // Then
        verify(personalRecordRepository)
                .upsert(
                        any(UUID.class),
                        eq(userId),
                        eq(exerciseId),
                        eq(PersonalRecordMetric.MAX_WEIGHT),
                        isNull(),
                        eq(100.0),
                        eq(setId),
                        eq(sessionId),
                        eq(PERFORMED_AT));

        assertThat(unlocked)
                .extracting(UnlockedAchievementResult::code)
                .containsExactlyInAnyOrder("PR_FIRST_MAX_WEIGHT", "PR_MAX_WEIGHT_PER_EXERCISE");
        assertThat(unlocked)
                .extracting(UnlockedAchievementResult::exerciseId)
                .containsExactlyInAnyOrder(null, exerciseId);
    }

    @Test
    @DisplayName("Should not unlock anything when the set only ties the record")
    void evaluateSets_ShouldNotUnlockOnATie() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        UUID setId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        AchievementDefinition perExercise =
                recordDefinition("PR_MAX_WEIGHT_PER_EXERCISE", AchievementScope.PER_EXERCISE);

        when(achievementDefinitionRepository.findByRuleTypeAndActiveTrue(RuleType.PERSONAL_RECORD))
                .thenReturn(List.of(perExercise));
        when(achievementStateLoader.load(eq(userId), any())).thenReturn(Map.of());

        var existing = PersonalRecord.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .exerciseId(exerciseId)
                .metric(PersonalRecordMetric.MAX_WEIGHT)
                .value(100.0)
                .build();
        when(personalRecordRepository.findByUserIdAndExerciseIdIn(eq(userId), any()))
                .thenReturn(List.of(existing));

        // When
        List<UnlockedAchievementResult> unlocked = personalRecordEvaluator.evaluateSets(
                userId, List.of(weightSet(setId, exerciseId, sessionId, 100.0, 10)));

        // Then
        assertThat(unlocked).isEmpty();
        verify(personalRecordRepository, never())
                .upsert(
                        any(UUID.class),
                        eq(userId),
                        eq(exerciseId),
                        eq(PersonalRecordMetric.MAX_WEIGHT),
                        isNull(),
                        anyDouble(),
                        any(UUID.class),
                        any(UUID.class),
                        any());
        verify(userAchievementRepository, never())
                .upsert(
                        any(UUID.class),
                        any(UUID.class),
                        any(UUID.class),
                        any(UUID.class),
                        any(),
                        anyBoolean(),
                        any(),
                        any(UUID.class),
                        any(UUID.class));
    }

    @Test
    @DisplayName("Should compare a later set of the session against the record an earlier one set")
    void evaluateSets_ShouldCompareTheSetsOfTheSessionInOrder() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(achievementDefinitionRepository.findByRuleTypeAndActiveTrue(RuleType.PERSONAL_RECORD))
                .thenReturn(List.of());
        when(achievementStateLoader.load(eq(userId), any())).thenReturn(Map.of());
        when(personalRecordRepository.findByUserIdAndExerciseIdIn(eq(userId), any()))
                .thenReturn(List.of());

        // When
        personalRecordEvaluator.evaluateSets(
                userId,
                List.of(
                        weightSet(UUID.randomUUID(), exerciseId, sessionId, 100.0, 10),
                        weightSet(UUID.randomUUID(), exerciseId, sessionId, 110.0, 10)));

        // Then
        ArgumentCaptor<Double> values = ArgumentCaptor.forClass(Double.class);
        verify(personalRecordRepository, org.mockito.Mockito.times(2))
                .upsert(
                        any(UUID.class),
                        eq(userId),
                        eq(exerciseId),
                        eq(PersonalRecordMetric.MAX_WEIGHT),
                        isNull(),
                        values.capture(),
                        any(),
                        eq(sessionId),
                        eq(PERFORMED_AT));
        assertThat(values.getAllValues()).containsExactly(100.0, 110.0);
    }

    @Test
    @DisplayName("Should keep a record per weight for the repetitions metric")
    void evaluateSets_ShouldStoreTheRepetitionsRecordPerWeight() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(achievementDefinitionRepository.findByRuleTypeAndActiveTrue(RuleType.PERSONAL_RECORD))
                .thenReturn(List.of());
        when(achievementStateLoader.load(eq(userId), any())).thenReturn(Map.of());
        when(personalRecordRepository.findByUserIdAndExerciseIdIn(eq(userId), any()))
                .thenReturn(List.of());

        // When
        personalRecordEvaluator.evaluateSets(
                userId, List.of(weightSet(UUID.randomUUID(), exerciseId, sessionId, 100.0, 10)));

        // Then
        verify(personalRecordRepository)
                .upsert(
                        any(UUID.class),
                        eq(userId),
                        eq(exerciseId),
                        eq(PersonalRecordMetric.MAX_REPS_AT_WEIGHT),
                        eq(100.0),
                        eq(10.0),
                        any(),
                        eq(sessionId),
                        eq(PERFORMED_AT));
    }

    @Test
    @DisplayName("Should rebuild the records of an exercise from the training history")
    void refreshExerciseRecords_ShouldRebuildTheRecordsOfTheExercise() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        when(trainingSetRepository.findCompletedSetsOfExercise(userId, exerciseId))
                .thenReturn(List.of(weightSet(UUID.randomUUID(), exerciseId, UUID.randomUUID(), 120.0, 8)));
        when(achievementDefinitionRepository.findByRuleTypeAndActiveTrue(RuleType.PERSONAL_RECORD))
                .thenReturn(List.of());
        when(achievementStateLoader.load(eq(userId), any())).thenReturn(Map.of());
        when(personalRecordRepository.findByUserIdAndExerciseIdIn(eq(userId), any()))
                .thenReturn(List.of());

        // When
        personalRecordEvaluator.refreshExerciseRecords(userId, exerciseId);

        // Then
        verify(personalRecordRepository).deleteByUserIdAndExerciseId(userId, exerciseId);
        verify(personalRecordRepository)
                .upsert(
                        any(UUID.class),
                        eq(userId),
                        eq(exerciseId),
                        eq(PersonalRecordMetric.MAX_WEIGHT),
                        isNull(),
                        eq(120.0),
                        any(),
                        any(),
                        eq(PERFORMED_AT));
    }

    @Test
    @DisplayName("Should not unlock an achievement that is already unlocked")
    void evaluateSets_ShouldNotUnlockAnAchievementTwice() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        UUID definitionId = UUID.randomUUID();

        AchievementDefinition perExercise =
                recordDefinition("PR_MAX_WEIGHT_PER_EXERCISE", AchievementScope.PER_EXERCISE);
        perExercise.setId(definitionId);

        UserAchievement alreadyUnlocked = UserAchievement.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .definition(perExercise)
                .exerciseId(exerciseId)
                .unlocked(true)
                .unlockedAt(PERFORMED_AT.minusSeconds(3600))
                .build();

        when(achievementDefinitionRepository.findByRuleTypeAndActiveTrue(RuleType.PERSONAL_RECORD))
                .thenReturn(List.of(perExercise));
        when(achievementStateLoader.load(eq(userId), any()))
                .thenReturn(Map.of(new AchievementKey(definitionId, exerciseId), alreadyUnlocked));
        when(personalRecordRepository.findByUserIdAndExerciseIdIn(eq(userId), any()))
                .thenReturn(List.of());

        // When
        List<UnlockedAchievementResult> unlocked = personalRecordEvaluator.evaluateSets(
                userId, List.of(weightSet(UUID.randomUUID(), exerciseId, UUID.randomUUID(), 100.0, 10)));

        // Then
        assertThat(unlocked).isEmpty();
        verify(userAchievementRepository, never())
                .upsert(any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any());
    }

    private AchievementDefinition recordDefinition(String code, AchievementScope scope) {
        AchievementDefinition definition = AchievementDefinition.builder()
                .code(code)
                .name(code)
                .category(AchievementCategory.PERSONAL_RECORD)
                .tier(AchievementTier.SILVER)
                .ruleType(RuleType.PERSONAL_RECORD)
                .metric(AchievementMetric.MAX_WEIGHT)
                .operator(ComparisonOperator.GTE)
                .scope(scope)
                .build();
        definition.setId(UUID.randomUUID());
        return definition;
    }

    private TrainingSetProjection weightSet(UUID setId, UUID exerciseId, UUID sessionId, double weightKg, int reps) {
        return new TrainingSetProjection(setId, exerciseId, sessionId, reps, weightKg, null, null, PERFORMED_AT);
    }
}
