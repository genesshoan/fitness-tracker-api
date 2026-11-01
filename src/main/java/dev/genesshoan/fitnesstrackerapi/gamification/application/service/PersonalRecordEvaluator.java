package dev.genesshoan.fitnesstrackerapi.gamification.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import dev.genesshoan.fitnesstrackerapi.common.calculator.OneRepMaxCalculator;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.UnlockedAchievementResult;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementDefinition;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementScope;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.PersonalRecordMetric;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.RuleType;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.UserAchievement;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.AchievementDefinitionRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.PersonalRecordRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.TrainingSetRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.UserAchievementRepository;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.projection.TrainingSetProjection;
import lombok.RequiredArgsConstructor;

/**
 * Evaluates the personal records of a user and the achievements that depend on
 * them.
 *
 * <p>This is the part of the rules that cannot be expressed as data. Comparing
 * against the previous best needs the value of the record, the granularity it
 * is kept at, and the training that set it.</p>
 *
 * <p>The sets are evaluated in the order they were performed, so a later set of
 * the same session is compared against the record an earlier one just set. A tie
 * does not improve a record, because a record has to be beaten to count.</p>
 */
@Component
@RequiredArgsConstructor
public class PersonalRecordEvaluator {

    private final AchievementDefinitionRepository achievementDefinitionRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final PersonalRecordRepository personalRecordRepository;
    private final TrainingSetRepository trainingSetRepository;
    private final AchievementStateLoader achievementStateLoader;

    /**
     * Evaluates the personal records of the given sets, in the order they were
     * performed.
     *
     * @param userId the user the records belong to
     * @param sets the sets to evaluate, in the order they were performed
     * @return the achievements unlocked by the sets
     */
    public List<UnlockedAchievementResult> evaluateSets(UUID userId, List<TrainingSetProjection> sets) {

        if (sets.isEmpty()) {
            return List.of();
        }

        Set<UUID> exerciseIds =
                sets.stream().map(TrainingSetProjection::exerciseId).collect(Collectors.toSet());
        Map<RecordKey, Double> records = loadRecordValues(userId, exerciseIds);

        List<AchievementDefinition> definitions =
                achievementDefinitionRepository.findByRuleTypeAndActiveTrue(RuleType.PERSONAL_RECORD);

        Map<PersonalRecordMetric, List<AchievementDefinition>> definitionsByMetric = definitions.stream()
                .filter(definition ->
                        definition.getMetric().personalRecordMetric().isPresent())
                .collect(Collectors.groupingBy(
                        definition ->
                                definition.getMetric().personalRecordMetric().orElseThrow(),
                        Collectors.toList()));

        Map<AchievementKey, UserAchievement> states = new HashMap<>(achievementStateLoader.load(userId, definitions));

        List<UnlockedAchievementResult> unlocked = new ArrayList<>();

        for (TrainingSetProjection set : sets) {
            for (RecordCandidate candidate : candidatesOf(set)) {
                RecordKey key = new RecordKey(set.exerciseId(), candidate.metric(), candidate.weightKg());
                Double current = records.get(key);

                if (current != null && candidate.value() <= current) {
                    continue;
                }

                personalRecordRepository.upsert(
                        UUID.randomUUID(),
                        userId,
                        set.exerciseId(),
                        candidate.metric(),
                        candidate.weightKg(),
                        candidate.value(),
                        set.setId(),
                        set.sessionId(),
                        set.performedAt());
                records.put(key, candidate.value());

                unlocked.addAll(unlockAchievements(userId, set, candidate, definitionsByMetric, states));
            }
        }

        return unlocked;
    }

    /**
     * Recalculates the records of an exercise from the training history.
     *
     * <p>The records are rebuilt from scratch, so a record that came from a set
     * that no longer exists disappears. The achievements that were unlocked
     * stay unlocked, because the achievement already happened.</p>
     *
     * @param userId the user the records belong to
     * @param exerciseId the exercise whose records are recalculated
     */
    public void refreshExerciseRecords(UUID userId, UUID exerciseId) {

        List<TrainingSetProjection> sets = trainingSetRepository.findCompletedSetsOfExercise(userId, exerciseId);

        personalRecordRepository.deleteByUserIdAndExerciseId(userId, exerciseId);

        evaluateSets(userId, sets);
    }

    private List<UnlockedAchievementResult> unlockAchievements(
            UUID userId,
            TrainingSetProjection set,
            RecordCandidate candidate,
            Map<PersonalRecordMetric, List<AchievementDefinition>> definitionsByMetric,
            Map<AchievementKey, UserAchievement> states) {

        List<UnlockedAchievementResult> unlocked = new ArrayList<>();

        for (AchievementDefinition definition : definitionsByMetric.getOrDefault(candidate.metric(), List.of())) {

            UUID scopeExerciseId = definition.getScope() == AchievementScope.PER_EXERCISE ? set.exerciseId() : null;
            AchievementKey key = new AchievementKey(definition.getId(), scopeExerciseId);
            UserAchievement existing = states.get(key);

            if (existing != null && existing.isUnlocked()) {
                continue;
            }

            UUID id = existing != null ? existing.getId() : UUID.randomUUID();
            Instant unlockedAt = set.performedAt();

            userAchievementRepository.upsert(
                    id,
                    userId,
                    definition.getId(),
                    scopeExerciseId,
                    null,
                    true,
                    unlockedAt,
                    set.sessionId(),
                    set.setId());

            states.put(
                    key,
                    UserAchievement.builder()
                            .id(id)
                            .userId(userId)
                            .definition(definition)
                            .exerciseId(scopeExerciseId)
                            .unlocked(true)
                            .unlockedAt(unlockedAt)
                            .build());

            unlocked.add(new UnlockedAchievementResult(
                    id, definition.getCode(), definition.getName(), definition.getTier(), scopeExerciseId, unlockedAt));
        }

        return unlocked;
    }

    private Map<RecordKey, Double> loadRecordValues(UUID userId, Set<UUID> exerciseIds) {

        Map<RecordKey, Double> records = new HashMap<>();
        for (var record : personalRecordRepository.findByUserIdAndExerciseIdIn(userId, exerciseIds)) {
            records.put(
                    new RecordKey(record.getExerciseId(), record.getMetric(), record.getWeightKg()), record.getValue());
        }
        return records;
    }

    /**
     * Returns the records the set can beat.
     *
     * <p>The weight records need both the weight and the repetitions, because
     * the estimated one repetition maximum is derived from them. The distance
     * and the duration stand on their own.</p>
     */
    private List<RecordCandidate> candidatesOf(TrainingSetProjection set) {

        List<RecordCandidate> candidates = new ArrayList<>();

        if (set.weightKg() != null && set.reps() != null) {
            candidates.add(new RecordCandidate(PersonalRecordMetric.MAX_WEIGHT, null, set.weightKg()));
            candidates.add(new RecordCandidate(
                    PersonalRecordMetric.MAX_ESTIMATED_1RM,
                    null,
                    OneRepMaxCalculator.calculate(set.reps(), set.weightKg())));
            candidates.add(new RecordCandidate(
                    PersonalRecordMetric.MAX_REPS_AT_WEIGHT,
                    set.weightKg(),
                    set.reps().doubleValue()));
        }

        if (set.distanceKm() != null) {
            candidates.add(new RecordCandidate(PersonalRecordMetric.MAX_DISTANCE, null, set.distanceKm()));
        }

        if (set.durationSeconds() != null) {
            candidates.add(new RecordCandidate(
                    PersonalRecordMetric.MAX_DURATION,
                    null,
                    set.durationSeconds().doubleValue()));
        }

        return candidates;
    }

    /**
     * Identifies a record of a user.
     *
     * @param exerciseId the exercise the record was set on
     * @param metric the metric the record was set for
     * @param weightKg the weight the record belongs to, or {@code null} for the
     *        metrics that are not compared per weight
     */
    private record RecordKey(UUID exerciseId, PersonalRecordMetric metric, Double weightKg) {}

    /**
     * A value reached by a set that can beat a record.
     *
     * @param metric the metric the value belongs to
     * @param weightKg the weight the value belongs to, or {@code null} for the
     *        metrics that are not compared per weight
     * @param value the value that was reached
     */
    private record RecordCandidate(PersonalRecordMetric metric, Double weightKg, double value) {}
}
