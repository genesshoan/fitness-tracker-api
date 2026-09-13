package dev.genesshoan.fitnesstrackerapi.stats.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.common.domain.ExerciseMetrics;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.mapper.ExerciseMetricsMapper;
import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.stats.calculator.AchievementCalculator;
import dev.genesshoan.fitnesstrackerapi.stats.calculator.OneRepMaxCalculator;
import dev.genesshoan.fitnesstrackerapi.stats.calculator.StreakCalculator;
import dev.genesshoan.fitnesstrackerapi.stats.calculator.StreakCalculator.StreakResult;
import dev.genesshoan.fitnesstrackerapi.stats.calculator.VolumeCalculator;
import dev.genesshoan.fitnesstrackerapi.stats.domain.Achievement;
import dev.genesshoan.fitnesstrackerapi.stats.domain.PersonalRecordHolder;
import dev.genesshoan.fitnesstrackerapi.stats.domain.PersonalRecordHolders;
import dev.genesshoan.fitnesstrackerapi.stats.dto.AchievementDTO;
import dev.genesshoan.fitnesstrackerapi.stats.dto.ExerciseProgressPointDTO;
import dev.genesshoan.fitnesstrackerapi.stats.dto.ExerciseProgressPointsDTO;
import dev.genesshoan.fitnesstrackerapi.stats.dto.OneRepMaxDTO;
import dev.genesshoan.fitnesstrackerapi.stats.dto.SessionVolumeDTO;
import dev.genesshoan.fitnesstrackerapi.stats.dto.StreakDTO;
import dev.genesshoan.fitnesstrackerapi.stats.mapper.AchievementMapper;
import dev.genesshoan.fitnesstrackerapi.stats.repository.StatsRepository;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.OneRepMaxProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.RankedSetProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.VolumeSetProjection;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import dev.genesshoan.fitnesstrackerapi.workout.repository.WorkoutSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final StatsRepository statsRepository;
    private final WorkoutSessionRepository workoutSessionRepository;
    private final ExerciseRepository exerciseRepository;

    private final ExerciseMetricsMapper exerciseMetricsMapper;
    private final AchievementMapper achievementMapper;

    public StreakDTO getCurrentStreak(UUID userId, Instant referenceInstant, String userTimezone) {

        ZoneId userZoneId = ZoneId.of(userTimezone);

        List<LocalDate> activeDaysAscending =
                statsRepository.getTrainedDatesBeforeAsc(userId, referenceInstant).stream()
                        .map(i -> i.atZone(userZoneId).toLocalDate())
                        .toList();

        LocalDate referenceDate = referenceInstant.atZone(userZoneId).toLocalDate();

        StreakResult streakResult = StreakCalculator.calculate(activeDaysAscending, referenceDate);

        LocalDate lastActiveDate = activeDaysAscending.isEmpty() ? null : activeDaysAscending.getLast();

        return new StreakDTO(streakResult.currentStreak(), streakResult.longestStreak(), lastActiveDate);
    }

    public SessionVolumeDTO calculateSessionVolume(UUID sessionId, UUID userId) {

        if (!workoutSessionRepository.existsByIdAndUserId(sessionId, userId)) {
            throw new ResourceNotFoundException("Workout session not found");
        }

        List<VolumeSetProjection> setVolumes = statsRepository.getSetsForVolume(sessionId, userId);

        return new SessionVolumeDTO(VolumeCalculator.calculate(setVolumes));
    }

    public OneRepMaxDTO getOneRepMax(UUID userId, UUID exerciseId) {

        if (!exerciseRepository.existsById(exerciseId)) {
            throw new ResourceNotFoundException("Exercise not found");
        }

        OneRepMaxProjection projection =
                statsRepository.getSetForOneRepMax(userId, exerciseId).orElse(new OneRepMaxProjection(0));

        return new OneRepMaxDTO(exerciseId, projection.estimatedOneRepMax());
    }

    public ExerciseProgressPointsDTO getExerciseProgress(
            UUID userId, UUID exerciseId, Instant from, Instant to, String userTimezone) {

        if (!exerciseRepository.existsById(exerciseId)) {
            throw new ResourceNotFoundException("Exercise not found");
        }

        ZoneId userZoneId = ZoneId.of(userTimezone);

        List<ExerciseProgressPointDTO> progress =
                statsRepository.getExerciseProgressPoints(userId, exerciseId, from, to).stream()
                        .map(p -> new ExerciseProgressPointDTO(
                                p.instant().atZone(userZoneId).toLocalDate(),
                                p.weightKg(),
                                p.reps(),
                                p.estimatedOneRepMax()))
                        .toList();

        return new ExerciseProgressPointsDTO(exerciseId, progress);
    }

    public Map<UUID, List<AchievementDTO>> calculateForSession(WorkoutSession session, UUID userId) {

        Map<UUID, List<AchievementDTO>> resultsBySetId = new HashMap<>();
        List<SessionSet> sessionSets = session.getExercises().stream()
                .flatMap(sessionExercise -> sessionExercise.getSets().stream())
                .filter(SessionSet::isCompleted)
                .toList();

        if (sessionSets.isEmpty()) return resultsBySetId;

        Set<UUID> exerciseIds = sessionSets.stream()
                .map(set -> set.getSessionExercise().getExercise().getId())
                .collect(Collectors.toSet());
        Instant startedAt = session.getStartedAt();
        Map<UUID, PersonalRecordHolders> recordsPerExercise =
                getPersonalRecordHoldersPerExercise(userId, exerciseIds, startedAt);

        for (SessionSet sessionSet : sessionSets) {
            UUID exerciseId = sessionSet.getSessionExercise().getExercise().getId();
            PersonalRecordHolders holders = recordsPerExercise.get(exerciseId);
            ExerciseMetrics metrics = exerciseMetricsMapper.toExerciseMetrics(sessionSet);
            List<Achievement> achievements = AchievementCalculator.compareData(metrics, holders);

            if (!achievements.isEmpty()) {
                resultsBySetId.put(sessionSet.getId(), achievementMapper.toDtos(achievements));
                updateHolders(holders, sessionSet, achievements);
            }
        }

        return resultsBySetId;
    }

    public List<AchievementDTO> calculateForSet(SessionSet sessionSet, UUID userId) {

        if (!sessionSet.isCompleted()) return List.of();

        PersonalRecordHolders holders = getPersonalRecordHolders(
                userId,
                sessionSet.getSessionExercise().getExercise().getId(),
                sessionSet.getSessionExercise().getWorkoutSession().getStartedAt());

        ExerciseMetrics metrics = exerciseMetricsMapper.toExerciseMetrics(sessionSet);

        List<Achievement> achievements = AchievementCalculator.compareData(metrics, holders);

        return achievementMapper.toDtos(achievements);
    }

    private void updateHolders(PersonalRecordHolders current, SessionSet set, List<Achievement> achievements) {

        for (Achievement achievement : achievements) {
            PersonalRecordHolder holder = new PersonalRecordHolder(
                    set.getId(), set.getSessionExercise().getWorkoutSession().getId(), achievement.value());

            switch (achievement.type()) {
                case NEW_MAX_WEIGHT -> current.setMaxWeight(holder);
                case NEW_ESTIMATED_1RM -> current.setMax1RM(holder);
                case NEW_MAX_DISTANCE -> current.setMaxDistance(holder);
                case NEW_MAX_DURATION -> current.setMaxDuration(holder);
                case MORE_REPS_AT_WEIGHT -> current.getRepsPerWeight().put(set.getWeightKg(), holder);
            }
        }
    }

    private PersonalRecordHolders getPersonalRecordHolders(UUID userId, UUID exerciseId, Instant startedAt) {

        Map<UUID, List<RankedSetProjection>> rankedSets =
                statsRepository.findRankedSets(userId, Set.of(exerciseId), startedAt);

        return buildPersonalRecordHolders(rankedSets.getOrDefault(exerciseId, List.of()));
    }

    private Map<UUID, PersonalRecordHolders> getPersonalRecordHoldersPerExercise(
            UUID userId, Set<UUID> exerciseIds, Instant startedAt) {
        Map<UUID, List<RankedSetProjection>> rankedSets =
                statsRepository.findRankedSets(userId, exerciseIds, startedAt);

        Map<UUID, PersonalRecordHolders> personalRecords = exerciseIds.stream()
                .collect(Collectors.toMap(
                        exerciseId -> exerciseId,
                        exerciseId -> buildPersonalRecordHolders(rankedSets.getOrDefault(exerciseId, List.of()))));

        return personalRecords;
    }

    private PersonalRecordHolders buildPersonalRecordHolders(List<RankedSetProjection> projections) {
        PersonalRecordHolders holders = new PersonalRecordHolders();

        for (RankedSetProjection projection : projections) {
            if (isTop(projection.rnWeight()) && projection.weightKg() != null) {
                holders.setMaxWeight(toHolder(projection, projection.weightKg()));
            }

            if (isTop(projection.rnOneRepMax()) && projection.reps() != null && projection.weightKg() != null) {
                holders.setMax1RM(
                        toHolder(projection, OneRepMaxCalculator.calculate(projection.reps(), projection.weightKg())));
            }

            if (isTop(projection.rnDistance()) && projection.distanceKm() != null) {
                holders.setMaxDistance(toHolder(projection, projection.distanceKm()));
            }

            if (isTop(projection.rnDuration()) && projection.durationSeconds() != null) {
                holders.setMaxDuration(
                        toHolder(projection, projection.durationSeconds().doubleValue()));
            }

            if (isTop(projection.rnRepsPerWeight()) && projection.weightKg() != null && projection.reps() != null) {
                holders.getRepsPerWeight()
                        .put(
                                projection.weightKg(),
                                toHolder(projection, projection.reps().doubleValue()));
            }
        }

        return holders;
    }

    private PersonalRecordHolder toHolder(RankedSetProjection projection, double value) {
        return new PersonalRecordHolder(projection.id(), projection.sessionId(), value);
    }

    private static boolean isTop(Integer rank) {
        return rank != null && rank == 1;
    }
}
