package dev.genesshoan.fitnesstrackerapi.stats.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.mapper.ExerciseMetricsMapper;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.ExerciseQueryUseCase;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.MuscleAssetUseCase;
import dev.genesshoan.fitnesstrackerapi.stats.application.port.in.TrainingMetricsUseCase;
import dev.genesshoan.fitnesstrackerapi.stats.application.port.in.result.TrainingSnapshot;
import dev.genesshoan.fitnesstrackerapi.stats.calculator.StreakCalculator;
import dev.genesshoan.fitnesstrackerapi.stats.calculator.StreakCalculator.StreakResult;
import dev.genesshoan.fitnesstrackerapi.stats.calculator.VolumeCalculator;
import dev.genesshoan.fitnesstrackerapi.stats.dto.ExerciseProgressPoint;
import dev.genesshoan.fitnesstrackerapi.stats.dto.ExerciseProgressPoints;
import dev.genesshoan.fitnesstrackerapi.stats.dto.MonthlyVolume;
import dev.genesshoan.fitnesstrackerapi.stats.dto.MuscleBaseAssets;
import dev.genesshoan.fitnesstrackerapi.stats.dto.MuscleIntensity;
import dev.genesshoan.fitnesstrackerapi.stats.dto.MuscleIntensityResponse;
import dev.genesshoan.fitnesstrackerapi.stats.dto.OneRepMax;
import dev.genesshoan.fitnesstrackerapi.stats.dto.SessionVolume;
import dev.genesshoan.fitnesstrackerapi.stats.dto.Streak;
import dev.genesshoan.fitnesstrackerapi.stats.repository.StatsRepository;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.MuscleIntensityProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.OneRepMaxProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.TrainingMetricsProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.VolumeSetProjection;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.GetUserTimeZoneUseCase;
import lombok.RequiredArgsConstructor;

/**
 * Provides user-scoped workout statistics and achievement calculations.
 *
 * <p>Statistics use completed sets and completed workout sessions only where
 * the underlying query requires historical performance. Date conversion uses
 * the user's configured timezone.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService implements TrainingMetricsUseCase {

    private final StatsRepository statsRepository;
    private final ExerciseQueryUseCase exerciseQueryUseCase;
    private final GetUserTimeZoneUseCase getUserTimeZoneUseCase;
    private final MuscleAssetUseCase muscleAssetUseCase;

    private final ExerciseMetricsMapper exerciseMetricsMapper;

    /**
     * Builds the training snapshot of a user.
     *
     * <p>The aggregates come from the training history, and the longest streak
     * is computed from the local days the user trained, in the timezone of the
     * user.</p>
     *
     * @param userId the user the aggregates belong to
     * @return the training aggregates of the user
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Override
    public TrainingSnapshot getSnapshot(UUID userId) {

        ZoneId userZoneId =
                getUserTimeZoneUseCase.getUserTimezoneById(userId).orElseThrow(ResourceNotFoundException::new);

        TrainingMetricsProjection metrics = statsRepository.getTrainingMetrics(userId);

        Instant referenceInstant = Instant.now();

        List<LocalDate> activeDaysAscending =
                statsRepository.getTrainedDatesBeforeAsc(userId, referenceInstant).stream()
                        .map(i -> i.atZone(userZoneId).toLocalDate())
                        .distinct()
                        .toList();

        StreakResult streakResult = StreakCalculator.calculate(
                activeDaysAscending, referenceInstant.atZone(userZoneId).toLocalDate());

        return new TrainingSnapshot(
                metrics.totalWorkouts(),
                metrics.totalSets(),
                metrics.totalReps(),
                metrics.totalVolumeKg(),
                metrics.totalTrainingSeconds(),
                metrics.totalDistanceKm(),
                metrics.distinctExercises(),
                streakResult.longestStreak());
    }

    /**
     * Calculates the current and longest consecutive workout-day streak.
     *
     * @param userId the user whose sessions are considered
     * @param userTimezone the user's IANA timezone
     * @return streak counts and the most recent active local date
     */
    public Streak getCurrentStreak(UUID userId, Instant referenceInstant) {

        ZoneId userZoneId =
                getUserTimeZoneUseCase.getUserTimezoneById(userId).orElseThrow(ResourceNotFoundException::new);

        List<LocalDate> activeDaysAscending =
                statsRepository.getTrainedDatesBeforeAsc(userId, referenceInstant).stream()
                        .map(i -> i.atZone(userZoneId).toLocalDate())
                        .distinct()
                        .toList();

        LocalDate referenceDate = referenceInstant.atZone(userZoneId).toLocalDate();

        StreakResult streakResult = StreakCalculator.calculate(activeDaysAscending, referenceDate);

        LocalDate lastActiveDate = activeDaysAscending.isEmpty() ? null : activeDaysAscending.getLast();

        return new Streak(streakResult.currentStreak(), streakResult.longestStreak(), lastActiveDate);
    }

    /**
     * Calculates strength volume for completed sets in a session.
     *
     * @param sessionId the session identifier
     * @param userId the authenticated owner
     * @return the sum of {@code weightKg * reps}; sets missing either value are excluded
     * @throws ResourceNotFoundException if the session is not owned by the user
     */
    public SessionVolume calculateSessionVolume(UUID sessionId, UUID userId) {

        if (!statsRepository.existsSession(sessionId, userId)) {
            throw new ResourceNotFoundException("Workout session not found");
        }

        List<VolumeSetProjection> setVolumes = statsRepository.getSetsForVolume(sessionId, userId);

        return new SessionVolume(VolumeCalculator.calculate(setVolumes));
    }

    /**
     * Calculates completed strength volume grouped by the user's calendar month.
     *
     * @param userId the authenticated owner
     * @param from inclusive local date range start
     * @param to inclusive local date range end
     * @param userTimezone the user's IANA timezone
     * @return chronological monthly volume entries; months without volume are omitted
     * @throws BadRequestException if {@code from} is after {@code to}
     */
    public List<MonthlyVolume> getMonthlyVolume(UUID userId, LocalDate from, LocalDate to) {

        if (from.isAfter(to)) {
            throw new BadRequestException("The start date cannot be after the end date");
        }

        ZoneId userTimezone =
                getUserTimeZoneUseCase.getUserTimezoneById(userId).orElseThrow(ResourceNotFoundException::new);
        Instant fromInclusive = from.atStartOfDay(userTimezone).toInstant();
        Instant toExclusive = to.plusDays(1).atStartOfDay(userTimezone).toInstant();

        return statsRepository.getMonthlyVolume(userId, fromInclusive, toExclusive, userTimezone.toString()).stream()
                .map(projection -> new MonthlyVolume(projection.month(), projection.volumeKg()))
                .toList();
    }

    /**
     * Calculates completed strength volume for an inclusive local-date range.
     *
     * @param userId the authenticated owner
     * @param from inclusive local date range start
     * @param to inclusive local date range end
     * @param userTimezone the user's IANA timezone
     * @return total volume in kilogram-repetitions
     * @throws BadRequestException if {@code from} is after {@code to}
     */
    public SessionVolume calculateVolume(UUID userId, LocalDate from, LocalDate to) {

        if (from.isAfter(to)) {
            throw new BadRequestException("The start date cannot be after the end date");
        }

        ZoneId userTimezone =
                getUserTimeZoneUseCase.getUserTimezoneById(userId).orElseThrow(ResourceNotFoundException::new);
        Instant fromInclusive = from.atStartOfDay(userTimezone).toInstant();
        Instant toExclusive = to.plusDays(1).atStartOfDay(userTimezone).toInstant();

        return new SessionVolume(statsRepository.getVolume(userId, fromInclusive, toExclusive));
    }

    /**
     * Finds the highest estimated one-repetition maximum for an exercise.
     *
     * <p>If the user has no qualifying completed set, the result is zero.
     *
     * @param userId the user whose history is searched
     * @param exerciseId the exercise identifier
     * @return the highest estimated 1RM, using the Epley formula
     * @throws ResourceNotFoundException if the exercise does not exist
     */
    public OneRepMax getOneRepMax(UUID userId, UUID exerciseId) {

        if (!exerciseQueryUseCase.existsById(exerciseId)) {
            throw new ResourceNotFoundException("Exercise not found");
        }

        OneRepMaxProjection projection =
                statsRepository.getSetForOneRepMax(userId, exerciseId).orElse(new OneRepMaxProjection(0));

        return new OneRepMax(exerciseId, projection.estimatedOneRepMax());
    }

    /**
     * Returns one best estimated-1RM point per completed session in a range.
     *
     * <p>{@code from} is inclusive and {@code to} is exclusive. Timestamps are
     * converted to local dates using the user's timezone.
     *
     * @param userId the user whose history is searched
     * @param exerciseId the exercise identifier
     * @param from inclusive range start
     * @param to exclusive range end
     * @param userTimezone the user's IANA timezone
     * @return chronological progress points
     * @throws BadRequestException if {@code from} is after {@code to}
     * @throws ResourceNotFoundException if the exercise does not exist
     */
    public ExerciseProgressPoints getExerciseProgress(UUID userId, UUID exerciseId, Instant from, Instant to) {

        if (from.isAfter(to)) {
            throw new BadRequestException("The start date cannot be after the end date");
        }

        if (!exerciseQueryUseCase.existsById(exerciseId)) {
            throw new ResourceNotFoundException("Exercise not found");
        }

        ZoneId userTimezone =
                getUserTimeZoneUseCase.getUserTimezoneById(userId).orElseThrow(ResourceNotFoundException::new);

        List<ExerciseProgressPoint> progress =
                statsRepository.getExerciseProgressPoints(userId, exerciseId, from, to).stream()
                        .map(p -> new ExerciseProgressPoint(
                                p.instant().atZone(userTimezone).toLocalDate(),
                                p.weightKg(),
                                p.reps(),
                                p.estimatedOneRepMax()))
                        .toList();

        return new ExerciseProgressPoints(exerciseId, progress);
    }

    /**
     * Returns range-relative application intensity for every catalog muscle.
     *
     * <p>The dates are inclusive in the user's configured timezone. This is the second of
     * two required normalization stages: the repository first scales each modality against
     * its own per-category maximum so different modalities stay comparable, and this method
     * then weights impact levels and scales every muscle to 0-10 against the user's own
     * strongest muscle in the window, so a score reads as "relative to my strongest muscle
     * in the window". Both stages are required, and the result is an application-level
     * visualization heuristic, not a physiological claim.
     *
     * @param userId the user whose completed training history is aggregated
     * @param from inclusive range start
     * @param to inclusive range end
     * @param userTimezone the user's IANA timezone
     * @return every muscle with metadata and a relative intensity score
     * @throws BadRequestException if {@code from} is after {@code to}
     */
    public MuscleIntensityResponse getMuscleIntensity(UUID userId, LocalDate from, LocalDate to) {

        if (from.isAfter(to)) {
            throw new BadRequestException("The start date cannot be after the end date");
        }

        ZoneId userTimezone =
                getUserTimeZoneUseCase.getUserTimezoneById(userId).orElseThrow(ResourceNotFoundException::new);
        Instant fromInclusive = from.atStartOfDay(userTimezone).toInstant();
        Instant toExclusive = to.plusDays(1).atStartOfDay(userTimezone).toInstant();

        Map<UUID, List<MuscleIntensityProjection>> projectionsByMuscle =
                statsRepository.getMuscleIntensity(userId, fromInclusive, toExclusive).stream()
                        .collect(Collectors.groupingBy(
                                MuscleIntensityProjection::muscleId, LinkedHashMap::new, Collectors.toList()));

        Map<UUID, Double> rawStimulusByMuscle = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<MuscleIntensityProjection>> entry : projectionsByMuscle.entrySet()) {
            double weightedStimulus = entry.getValue().stream()
                    .mapToDouble(projection -> projection.impactLevel() == null
                            ? 0.0
                            : projection.rawStimulus()
                                    * projection.impactLevel().weight())
                    .sum();
            rawStimulusByMuscle.put(entry.getKey(), weightedStimulus);
        }
        double maximumStimulus = rawStimulusByMuscle.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        List<MuscleIntensity> muscles = projectionsByMuscle.entrySet().stream()
                .map(entry -> {
                    MuscleIntensityProjection metadata = entry.getValue().getFirst();
                    double intensity = maximumStimulus == 0.0
                            ? 0.0
                            : BigDecimal.valueOf(rawStimulusByMuscle.get(entry.getKey()) / maximumStimulus * 10.0)
                                    .setScale(1, RoundingMode.HALF_UP)
                                    .doubleValue();

                    String frontAssetUrl = muscleAssetUseCase.resolveMuscleUrl(metadata.frontObjectKey());
                    String backAssetUrl = muscleAssetUseCase.resolveMuscleUrl(metadata.backObjectKey());

                    return new MuscleIntensity(
                            metadata.muscleId(),
                            metadata.name(),
                            metadata.slug(),
                            metadata.bodyRegion(),
                            frontAssetUrl,
                            backAssetUrl,
                            intensity);
                })
                .toList();

        var baseAssets = muscleAssetUseCase.findBaseAssets();

        return new MuscleIntensityResponse(
                from, to, new MuscleBaseAssets(baseAssets.frontBaseAssetUrl(), baseAssets.backBaseAssetUrl()), muscles);
    }

    private static boolean isTop(Integer rank) {
        return rank != null && rank == 1;
    }
}
