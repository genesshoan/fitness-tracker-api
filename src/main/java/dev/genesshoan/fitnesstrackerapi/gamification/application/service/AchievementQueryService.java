package dev.genesshoan.fitnesstrackerapi.gamification.application.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.ExerciseQueryUseCase;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.AchievementCatalogUseCase;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.AchievementFeedUseCase;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.AchievementCatalogResult;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.AchievementFeedResult;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.AchievementSummaryResult;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.AchievementSummaryResult.AchievementCategorySummary;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.AchievementSummaryResult.NextAchievement;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementCategory;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementDefinition;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementScope;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.PersonalRecord;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.PersonalRecordMetric;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.UserAchievement;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.AchievementDefinitionRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.PersonalRecordRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.UserAchievementRepository;
import lombok.RequiredArgsConstructor;

/**
 * Reads the catalog of achievements, the feed of unlocked achievements, and the
 * state of the collection of a user.
 *
 * <p>Everything is read from the state that the evaluation persisted, so a read
 * never recomputes anything.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AchievementQueryService implements AchievementCatalogUseCase, AchievementFeedUseCase {

    private static final int NEXT_ACHIEVEMENTS = 3;

    private final AchievementDefinitionRepository achievementDefinitionRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final PersonalRecordRepository personalRecordRepository;
    private final ExerciseQueryUseCase exerciseQueryUseCase;

    @Override
    public List<AchievementCatalogResult> getCatalog(
            UUID userId, AchievementCategory category, boolean unlockedOnly, boolean unseenOnly) {

        List<AchievementDefinition> definitions = activeDefinitions(category);
        Map<AchievementKey, UserAchievement> states = statesOf(userId, definitions);
        Map<UUID, Map<PersonalRecordMetric, Double>> records = recordsOf(userId);
        Map<UUID, String> exerciseNames = exerciseNames(states.values());

        List<AchievementCatalogResult> results = new ArrayList<>();

        for (AchievementDefinition definition : definitions) {
            if (definition.getScope() == AchievementScope.GLOBAL) {
                results.add(toCatalogResult(
                        definition, states.get(new AchievementKey(definition.getId(), null)), null, null, records));
                continue;
            }
            states.values().stream()
                    .filter(state ->
                            definition.getId().equals(state.getDefinition().getId()))
                    .forEach(state -> results.add(toCatalogResult(
                            definition,
                            state,
                            state.getExerciseId(),
                            exerciseNames.get(state.getExerciseId()),
                            records)));
        }

        return results.stream()
                .filter(result -> !unlockedOnly || result.unlocked())
                .filter(result -> !unseenOnly || (result.unlocked() && !result.seen()))
                .toList();
    }

    @Override
    public List<AchievementCatalogResult> getByCode(UUID userId, String code) {

        AchievementDefinition definition = achievementDefinitionRepository
                .findByCode(code)
                .filter(AchievementDefinition::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement not found"));

        return getCatalog(userId, definition.getCategory(), false, false).stream()
                .filter(result -> result.code().equals(code))
                .toList();
    }

    @Override
    public List<AchievementFeedResult> getFeed(UUID userId, boolean unseenOnly, int limit) {

        List<UserAchievement> unlocked = unseenOnly
                ? userAchievementRepository.findByUserIdAndUnlockedTrueAndSeenFalseOrderByUnlockedAtDesc(
                        userId, PageRequest.of(0, Math.max(1, limit)))
                : userAchievementRepository.findByUserIdAndUnlockedTrueOrderByUnlockedAtDesc(
                        userId, PageRequest.of(0, Math.max(1, limit)));

        Map<UUID, String> exerciseNames = exerciseNames(unlocked);

        return unlocked.stream()
                .map(state -> new AchievementFeedResult(
                        state.getId(),
                        state.getDefinition().getCode(),
                        state.getDefinition().getName(),
                        state.getDefinition().getTier(),
                        state.getDefinition().getCategory(),
                        state.getExerciseId(),
                        exerciseNames.get(state.getExerciseId()),
                        state.getUnlockedAt(),
                        state.isSeen()))
                .toList();
    }

    @Override
    @Transactional
    public void markSeen(UUID userId, List<UUID> achievementIds) {

        if (achievementIds == null || achievementIds.isEmpty()) {
            return;
        }

        userAchievementRepository.markSeen(userId, achievementIds);
    }

    @Override
    public AchievementSummaryResult getSummary(UUID userId) {

        List<AchievementDefinition> definitions = achievementDefinitionRepository.findByActiveTrueOrderBySortOrderAsc();
        Map<AchievementKey, UserAchievement> states = statesOf(userId, definitions);

        long totalUnlocked = definitions.stream()
                .filter(definition -> states.values().stream()
                        .anyMatch(state -> state.isUnlocked()
                                && definition
                                        .getId()
                                        .equals(state.getDefinition().getId())))
                .count();

        long unseenCount = states.values().stream()
                .filter(state -> state.isUnlocked() && !state.isSeen())
                .count();

        Map<AchievementCategory, AchievementCategorySummary> byCategory = definitions.stream()
                .collect(Collectors.groupingBy(
                        AchievementDefinition::getCategory,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                group -> new AchievementCategorySummary(
                                        (long) group.stream()
                                                .filter(definition -> states.values().stream()
                                                        .anyMatch(state -> state.isUnlocked()
                                                                && definition
                                                                        .getId()
                                                                        .equals(state.getDefinition()
                                                                                .getId())))
                                                .count(),
                                        (long) group.size()))));

        List<NextAchievement> next = definitions.stream()
                .filter(definition -> definition.getScope() == AchievementScope.GLOBAL)
                .filter(definition -> definition.getThreshold() != null)
                .filter(definition -> states.values().stream()
                        .noneMatch(state -> state.isUnlocked()
                                && definition
                                        .getId()
                                        .equals(state.getDefinition().getId())))
                .map(definition -> {
                    UserAchievement state = states.get(new AchievementKey(definition.getId(), null));
                    Double progress = state == null ? null : state.getProgressValue();
                    Double threshold = definition.getThreshold();
                    Double remaining = progress == null ? threshold : Math.max(0.0, threshold - progress);
                    return new NextAchievement(
                            definition.getCode(), definition.getName(), progress, threshold, remaining);
                })
                .sorted(Comparator.comparingDouble((NextAchievement achievement) ->
                                achievement.progressValue() == null || achievement.threshold() == null
                                        ? 0.0
                                        : achievement.progressValue() / achievement.threshold())
                        .reversed())
                .limit(NEXT_ACHIEVEMENTS)
                .toList();

        return new AchievementSummaryResult(definitions.size(), totalUnlocked, unseenCount, byCategory, next);
    }

    private List<AchievementDefinition> activeDefinitions(AchievementCategory category) {
        return achievementDefinitionRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .filter(definition -> category == null || definition.getCategory() == category)
                .toList();
    }

    private Map<AchievementKey, UserAchievement> statesOf(UUID userId, List<AchievementDefinition> definitions) {

        if (definitions.isEmpty()) {
            return Map.of();
        }

        Map<AchievementKey, UserAchievement> states = new HashMap<>();
        for (UserAchievement state : userAchievementRepository.findByUserIdAndDefinitionIdIn(
                userId, definitions.stream().map(AchievementDefinition::getId).toList())) {
            states.putIfAbsent(new AchievementKey(state.getDefinition().getId(), state.getExerciseId()), state);
        }
        return states;
    }

    private Map<UUID, Map<PersonalRecordMetric, Double>> recordsOf(UUID userId) {

        Map<UUID, Map<PersonalRecordMetric, Double>> records = new HashMap<>();
        for (PersonalRecord record : personalRecordRepository.findByUserId(userId)) {
            records.computeIfAbsent(record.getExerciseId(), key -> new HashMap<>())
                    .merge(record.getMetric(), record.getValue(), Math::max);
        }
        return records;
    }

    private Map<UUID, String> exerciseNames(Collection<UserAchievement> states) {

        Set<UUID> exerciseIds = states.stream()
                .map(UserAchievement::getExerciseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (exerciseIds.isEmpty()) {
            return Map.of();
        }

        return exerciseQueryUseCase.findByIds(exerciseIds).values().stream()
                .collect(Collectors.toMap(Exercise::getId, Exercise::getName));
    }

    private AchievementCatalogResult toCatalogResult(
            AchievementDefinition definition,
            UserAchievement state,
            UUID exerciseId,
            String exerciseName,
            Map<UUID, Map<PersonalRecordMetric, Double>> records) {

        Double currentValue = exerciseId == null
                ? null
                : definition
                        .getMetric()
                        .personalRecordMetric()
                        .map(metric ->
                                records.getOrDefault(exerciseId, Map.of()).get(metric))
                        .orElse(null);

        return new AchievementCatalogResult(
                definition.getCode(),
                definition.getName(),
                definition.getDescription(),
                definition.getCategory(),
                definition.getTier(),
                definition.getRuleType(),
                definition.getMetric(),
                definition.getThreshold(),
                definition.getScope(),
                definition.getIconKey(),
                state == null ? null : state.getId(),
                exerciseId,
                exerciseName,
                state == null ? null : state.getProgressValue(),
                currentValue,
                state != null && state.isUnlocked(),
                state == null ? null : state.getUnlockedAt(),
                state != null && state.isSeen());
    }
}
