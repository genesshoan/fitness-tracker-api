package dev.genesshoan.fitnesstrackerapi.stats.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.mapper.ExerciseMetricsMapper;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.ExerciseQueryUseCase;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.MuscleAssetUseCase;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.MuscleBaseAssetsResult;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.BodyRegion;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.stats.application.port.in.result.TrainingSnapshot;
import dev.genesshoan.fitnesstrackerapi.stats.dto.ExerciseProgressPoints;
import dev.genesshoan.fitnesstrackerapi.stats.dto.MonthlyVolume;
import dev.genesshoan.fitnesstrackerapi.stats.dto.MuscleIntensity;
import dev.genesshoan.fitnesstrackerapi.stats.dto.MuscleIntensityResponse;
import dev.genesshoan.fitnesstrackerapi.stats.dto.OneRepMax;
import dev.genesshoan.fitnesstrackerapi.stats.dto.SessionVolume;
import dev.genesshoan.fitnesstrackerapi.stats.dto.Streak;
import dev.genesshoan.fitnesstrackerapi.stats.repository.StatsRepository;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.ExerciseProgressProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.MonthlyVolumeProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.MuscleIntensityProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.OneRepMaxProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.TrainingMetricsProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.VolumeSetProjection;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.GetUserTimeZoneUseCase;
import net.datafaker.Faker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    private static final Faker FAKER = new Faker();
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final ZoneId UTC = ZoneId.of("UTC");

    @Mock
    private StatsRepository statsRepository;

    @Mock
    private ExerciseQueryUseCase exerciseQueryUseCase;

    @Mock
    private GetUserTimeZoneUseCase getUserTimeZoneUseCase;

    @Mock
    private ExerciseMetricsMapper exerciseMetricsMapper;

    @Mock
    private MuscleAssetUseCase muscleAssetUseCase;

    @InjectMocks
    private StatsService statsService;

    @Test
    @DisplayName("Should calculate current streak in the user's timezone")
    void getCurrentStreak_shouldCalculateStreakInUserTimezone() {
        UUID userId = UUID.randomUUID();
        Instant reference = Instant.parse("2026-09-13T01:00:00Z");
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(SAO_PAULO));
        when(statsRepository.getTrainedDatesBeforeAsc(userId, reference))
                .thenReturn(List.of(Instant.parse("2026-09-11T23:00:00Z"), Instant.parse("2026-09-12T23:00:00Z")));

        Streak result = statsService.getCurrentStreak(userId, reference);

        assertThat(result.currentStreak()).isEqualTo(2);
        assertThat(result.longestStreak()).isEqualTo(2);
        assertThat(result.lastActiveDay()).isEqualTo(LocalDate.of(2026, 9, 12));
        verify(statsRepository).getTrainedDatesBeforeAsc(userId, reference);
    }

    @Test
    @DisplayName("Should return an empty streak when the user has no trained dates")
    void getCurrentStreak_shouldReturnEmptyStreakWhenNoDatesExist() {
        UUID userId = UUID.randomUUID();
        Instant reference = Instant.parse("2026-09-13T01:00:00Z");
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(UTC));
        when(statsRepository.getTrainedDatesBeforeAsc(userId, reference)).thenReturn(List.of());

        Streak result = statsService.getCurrentStreak(userId, reference);

        assertThat(result).isEqualTo(new Streak(0, 0, null));
    }

    @Test
    @DisplayName("Should build the training snapshot with the aggregates and the longest streak")
    void getSnapshot_shouldCombineTheAggregatesAndTheLongestStreak() {
        UUID userId = UUID.randomUUID();
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(SAO_PAULO));
        when(statsRepository.getTrainingMetrics(userId))
                .thenReturn(new TrainingMetricsProjection(12, 40, 300, 5000.0, 7200, 15.5, 6));
        when(statsRepository.getTrainedDatesBeforeAsc(eq(userId), any()))
                .thenReturn(List.of(
                        Instant.parse("2026-09-10T01:00:00Z"),
                        Instant.parse("2026-09-11T01:00:00Z"),
                        Instant.parse("2026-09-12T01:00:00Z")));

        TrainingSnapshot snapshot = statsService.getSnapshot(userId);

        assertThat(snapshot.totalWorkouts()).isEqualTo(12);
        assertThat(snapshot.totalSets()).isEqualTo(40);
        assertThat(snapshot.totalReps()).isEqualTo(300);
        assertThat(snapshot.totalVolumeKg()).isEqualTo(5000.0);
        assertThat(snapshot.totalTrainingSeconds()).isEqualTo(7200);
        assertThat(snapshot.totalDistanceKm()).isEqualTo(15.5);
        assertThat(snapshot.distinctExercises()).isEqualTo(6);
        assertThat(snapshot.longestStreak()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should return an empty snapshot for a user without training")
    void getSnapshot_shouldReturnZeroesForAUserWithoutTraining() {
        UUID userId = UUID.randomUUID();
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(UTC));
        when(statsRepository.getTrainingMetrics(userId))
                .thenReturn(new TrainingMetricsProjection(0, 0, 0, 0.0, 0, 0.0, 0));
        when(statsRepository.getTrainedDatesBeforeAsc(eq(userId), any())).thenReturn(List.of());

        TrainingSnapshot snapshot = statsService.getSnapshot(userId);

        assertThat(snapshot.totalWorkouts()).isZero();
        assertThat(snapshot.totalSets()).isZero();
        assertThat(snapshot.longestStreak()).isZero();
    }

    @Test
    @DisplayName("Should fail to build a snapshot for a user without a timezone")
    void getSnapshot_shouldFailForAUserWithoutTimezone() {
        UUID userId = UUID.randomUUID();
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statsService.getSnapshot(userId)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(statsRepository);
    }

    @Test
    @DisplayName("Should reset current streak when the last workout is not recent")
    void getCurrentStreak_shouldResetCurrentStreakWhenLastDateIsOlderThanYesterday() {
        UUID userId = UUID.randomUUID();
        Instant reference = Instant.parse("2026-09-13T12:00:00Z");
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(UTC));
        when(statsRepository.getTrainedDatesBeforeAsc(userId, reference))
                .thenReturn(List.of(
                        Instant.parse("2026-09-09T12:00:00Z"),
                        Instant.parse("2026-09-10T12:00:00Z"),
                        Instant.parse("2026-09-11T12:00:00Z")));

        Streak result = statsService.getCurrentStreak(userId, reference);

        assertThat(result.currentStreak()).isZero();
        assertThat(result.longestStreak()).isEqualTo(3);
        assertThat(result.lastActiveDay()).isEqualTo(LocalDate.of(2026, 9, 11));
    }

    @Test
    @DisplayName("Should count a single trained day once when several sessions share the day")
    void getCurrentStreak_shouldCountDuplicatedDaysOnlyOnce() {
        UUID userId = UUID.randomUUID();
        Instant reference = Instant.parse("2026-09-13T01:00:00Z");
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(SAO_PAULO));
        when(statsRepository.getTrainedDatesBeforeAsc(userId, reference))
                .thenReturn(List.of(
                        Instant.parse("2026-09-12T10:00:00Z"),
                        Instant.parse("2026-09-12T20:00:00Z"),
                        Instant.parse("2026-09-12T23:00:00Z")));

        Streak result = statsService.getCurrentStreak(userId, reference);

        assertThat(result.currentStreak()).isEqualTo(1);
        assertThat(result.longestStreak()).isEqualTo(1);
        assertThat(result.lastActiveDay()).isEqualTo(LocalDate.of(2026, 9, 12));
    }

    @Test
    @DisplayName("Should reject streak calculation when the user has no timezone configured")
    void getCurrentStreak_shouldThrowWhenUserTimezoneIsMissing() {
        UUID userId = UUID.randomUUID();
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statsService.getCurrentStreak(userId, Instant.parse("2026-09-13T01:00:00Z")))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(statsRepository);
    }

    @Test
    @DisplayName("Should calculate session volume when session belongs to user")
    void calculateSessionVolume_shouldReturnCalculatedVolume() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(statsRepository.existsSession(sessionId, userId)).thenReturn(true);
        when(statsRepository.getSetsForVolume(sessionId, userId))
                .thenReturn(List.of(new VolumeSetProjection(50.0, 10), new VolumeSetProjection(40.0, 5)));

        SessionVolume result = statsService.calculateSessionVolume(sessionId, userId);

        assertThat(result.volumeKg()).isEqualTo(700.0);
        verify(statsRepository).getSetsForVolume(sessionId, userId);
    }

    @Test
    @DisplayName("Should calculate volume for an inclusive local-date range")
    void calculateVolume_shouldConvertDatesAndReturnVolume() {
        UUID userId = UUID.randomUUID();
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(SAO_PAULO));
        when(statsRepository.getVolume(
                        userId, Instant.parse("2026-01-01T03:00:00Z"), Instant.parse("2026-02-01T03:00:00Z")))
                .thenReturn(700.0);

        assertThat(statsService.calculateVolume(userId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .isEqualTo(new SessionVolume(700.0));
    }

    @Test
    @DisplayName("Should calculate monthly volume in repository order")
    void getMonthlyVolume_shouldMapRepositoryResults() {
        UUID userId = UUID.randomUUID();
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(UTC));
        when(statsRepository.getMonthlyVolume(
                        userId, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-02-01T00:00:00Z"), "UTC"))
                .thenReturn(List.of(new MonthlyVolumeProjection("2026-01", 500.0)));

        assertThat(statsService.getMonthlyVolume(userId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .containsExactly(new MonthlyVolume("2026-01", 500.0));
    }

    @Test
    @DisplayName("Should convert monthly volume dates to the user's timezone")
    void getMonthlyVolume_shouldConvertDatesInUserTimezone() {
        UUID userId = UUID.randomUUID();
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(SAO_PAULO));
        when(statsRepository.getMonthlyVolume(
                        userId,
                        Instant.parse("2026-01-01T03:00:00Z"),
                        Instant.parse("2026-02-01T03:00:00Z"),
                        "America/Sao_Paulo"))
                .thenReturn(List.of());

        assertThat(statsService.getMonthlyVolume(userId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .isEmpty();

        verify(statsRepository)
                .getMonthlyVolume(
                        userId,
                        Instant.parse("2026-01-01T03:00:00Z"),
                        Instant.parse("2026-02-01T03:00:00Z"),
                        "America/Sao_Paulo");
    }

    @Test
    @DisplayName("Should reject volume ranges when start date is after end date")
    void calculateVolume_shouldThrowWhenFromIsAfterTo() {
        assertThatThrownBy(() -> statsService.calculateVolume(
                        UUID.randomUUID(), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The start date cannot be after the end date");

        verifyNoInteractions(statsRepository, getUserTimeZoneUseCase);
    }

    @Test
    @DisplayName("Should reject monthly volume ranges when start date is after end date")
    void getMonthlyVolume_shouldThrowWhenFromIsAfterTo() {
        assertThatThrownBy(() -> statsService.getMonthlyVolume(
                        UUID.randomUUID(), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The start date cannot be after the end date");

        verifyNoInteractions(statsRepository, getUserTimeZoneUseCase);
    }

    @Test
    @DisplayName("Should reject volume calculation when the user has no timezone configured")
    void calculateVolume_shouldThrowWhenUserTimezoneIsMissing() {
        UUID userId = UUID.randomUUID();
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> statsService.calculateVolume(userId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(statsRepository);
    }

    @Test
    @DisplayName("Should return empty exercise progress when no points exist")
    void getExerciseProgress_shouldReturnEmptyProgress() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");
        when(exerciseQueryUseCase.existsById(exerciseId)).thenReturn(true);
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(UTC));
        when(statsRepository.getExerciseProgressPoints(any(), any(), any(), any()))
                .thenReturn(List.of());

        assertThat(statsService.getExerciseProgress(userId, exerciseId, from, to))
                .isEqualTo(new ExerciseProgressPoints(exerciseId, List.of()));
    }

    @Test
    @DisplayName("Should reject progress for an unknown exercise")
    void getExerciseProgress_shouldThrowWhenExerciseDoesNotExist() {
        UUID exerciseId = UUID.randomUUID();
        when(exerciseQueryUseCase.existsById(exerciseId)).thenReturn(false);

        assertThatThrownBy(() -> statsService.getExerciseProgress(
                        UUID.randomUUID(),
                        exerciseId,
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-02-01T00:00:00Z")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Exercise not found");

        verifyNoInteractions(statsRepository, getUserTimeZoneUseCase);
    }

    @Test
    @DisplayName("Should reject progress when start date is after end date")
    void getExerciseProgress_shouldThrowWhenFromIsAfterTo() {
        UUID exerciseId = UUID.randomUUID();
        Instant from = Instant.parse("2026-02-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-01T00:00:00Z");

        assertThatThrownBy(() -> statsService.getExerciseProgress(UUID.randomUUID(), exerciseId, from, to))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The start date cannot be after the end date");

        verifyNoInteractions(statsRepository, exerciseQueryUseCase, getUserTimeZoneUseCase);
    }

    @Test
    @DisplayName("Should reject progress when the user has no timezone configured")
    void getExerciseProgress_shouldThrowWhenUserTimezoneIsMissing() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        when(exerciseQueryUseCase.existsById(exerciseId)).thenReturn(true);
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statsService.getExerciseProgress(
                        userId,
                        exerciseId,
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-02-01T00:00:00Z")))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(statsRepository);
    }

    @Test
    @DisplayName("Should reject session volume for an unknown session")
    void calculateSessionVolume_shouldThrowWhenSessionDoesNotBelongToUser() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(statsRepository.existsSession(sessionId, userId)).thenReturn(false);

        assertThatThrownBy(() -> statsService.calculateSessionVolume(sessionId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Workout session not found");

        verify(statsRepository).existsSession(sessionId, userId);
        verify(statsRepository, never()).getSetsForVolume(any(UUID.class), any(UUID.class));
    }

    @Test
    @DisplayName("Should return the user's one rep max")
    void getOneRepMax_shouldReturnProjectionValue() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        when(exerciseQueryUseCase.existsById(exerciseId)).thenReturn(true);
        when(statsRepository.getSetForOneRepMax(userId, exerciseId))
                .thenReturn(Optional.of(new OneRepMaxProjection(100.0)));

        OneRepMax result = statsService.getOneRepMax(userId, exerciseId);

        assertThat(result).isEqualTo(new OneRepMax(exerciseId, 100.0));
    }

    @Test
    @DisplayName("Should return zero one rep max when no completed sets exist")
    void getOneRepMax_shouldReturnZeroWhenNoSetExists() {
        UUID exerciseId = UUID.randomUUID();
        when(exerciseQueryUseCase.existsById(exerciseId)).thenReturn(true);
        when(statsRepository.getSetForOneRepMax(any(), any())).thenReturn(Optional.empty());

        assertThat(statsService.getOneRepMax(UUID.randomUUID(), exerciseId).estimatedOneRepMax())
                .isZero();
    }

    @Test
    @DisplayName("Should reject one rep max for an unknown exercise")
    void getOneRepMax_shouldThrowWhenExerciseDoesNotExist() {
        UUID exerciseId = UUID.randomUUID();
        when(exerciseQueryUseCase.existsById(exerciseId)).thenReturn(false);

        assertThatThrownBy(() -> statsService.getOneRepMax(UUID.randomUUID(), exerciseId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Exercise not found");

        verifyNoInteractions(statsRepository);
    }

    @Test
    @DisplayName("Should map exercise progress dates to the user's timezone")
    void getExerciseProgress_shouldMapProgressPoints() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");
        when(exerciseQueryUseCase.existsById(exerciseId)).thenReturn(true);
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(SAO_PAULO));
        when(statsRepository.getExerciseProgressPoints(userId, exerciseId, from, to))
                .thenReturn(
                        List.of(new ExerciseProgressProjection(Instant.parse("2026-01-15T01:00:00Z"), 80.0, 5, 93.33)));

        ExerciseProgressPoints result = statsService.getExerciseProgress(userId, exerciseId, from, to);

        assertThat(result.exerciseId()).isEqualTo(exerciseId);
        assertThat(result.progress()).hasSize(1);
        assertThat(result.progress().getFirst().date()).isEqualTo(LocalDate.of(2026, 1, 14));
        assertThat(result.progress().getFirst().weightKg()).isEqualTo(80.0);
        assertThat(result.progress().getFirst().reps()).isEqualTo(5);
        assertThat(result.progress().getFirst().estimatedOneRepMax()).isEqualTo(93.33);
    }

    @Test
    @DisplayName("Should return zero intensity for every muscle when no stimulus exists")
    void getMuscleIntensity_shouldReturnZeroForEveryMuscle() {
        UUID userId = UUID.randomUUID();
        UUID firstMuscleId = UUID.randomUUID();
        UUID secondMuscleId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 7);
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(UTC));
        when(statsRepository.getMuscleIntensity(
                        userId, Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-08T00:00:00Z")))
                .thenReturn(List.of(
                        new MuscleIntensityProjection(
                                firstMuscleId, "First muscle", "first-muscle", BodyRegion.ARMS, null, null, null, 0.0),
                        new MuscleIntensityProjection(
                                secondMuscleId,
                                "Second muscle",
                                "second-muscle",
                                BodyRegion.LEGS,
                                null,
                                null,
                                null,
                                0.0)));

        when(muscleAssetUseCase.resolveMuscleUrl(any())).thenReturn(null);
        when(muscleAssetUseCase.findBaseAssets()).thenReturn(new MuscleBaseAssetsResult(null, null));

        MuscleIntensityResponse result = statsService.getMuscleIntensity(userId, from, to);

        assertThat(result.from()).isEqualTo(from);
        assertThat(result.to()).isEqualTo(to);
        assertThat(result.muscles())
                .containsExactly(
                        new MuscleIntensity(
                                firstMuscleId, "First muscle", "first-muscle", BodyRegion.ARMS, null, null, 0.0),
                        new MuscleIntensity(
                                secondMuscleId, "Second muscle", "second-muscle", BodyRegion.LEGS, null, null, 0.0));
    }

    @Test
    @DisplayName("Should weight impact levels and normalize the highest muscle to ten")
    void getMuscleIntensity_shouldWeightImpactLevelsAndNormalize() {
        UUID userId = UUID.randomUUID();
        UUID weightedMuscleId = UUID.randomUUID();
        UUID highestMuscleId = UUID.randomUUID();
        UUID zeroMuscleId = UUID.randomUUID();
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(UTC));
        when(statsRepository.getMuscleIntensity(userId, Instant.EPOCH, Instant.EPOCH.plusSeconds(86400)))
                .thenReturn(List.of(
                        new MuscleIntensityProjection(
                                weightedMuscleId,
                                "Weighted muscle",
                                "weighted-muscle",
                                BodyRegion.BACK,
                                null,
                                null,
                                ImpactLevel.PRIMARY,
                                8.0),
                        new MuscleIntensityProjection(
                                weightedMuscleId,
                                "Weighted muscle",
                                "weighted-muscle",
                                BodyRegion.BACK,
                                null,
                                null,
                                ImpactLevel.SECONDARY,
                                4.0),
                        new MuscleIntensityProjection(
                                weightedMuscleId,
                                "Weighted muscle",
                                "weighted-muscle",
                                BodyRegion.BACK,
                                null,
                                null,
                                ImpactLevel.STABILIZER,
                                4.0),
                        new MuscleIntensityProjection(
                                highestMuscleId,
                                "Highest muscle",
                                "highest-muscle",
                                BodyRegion.LEGS,
                                null,
                                null,
                                ImpactLevel.PRIMARY,
                                5.0),
                        new MuscleIntensityProjection(
                                zeroMuscleId, "Zero muscle", "zero-muscle", BodyRegion.OTHER, null, null, null, 0.0)));

        when(muscleAssetUseCase.resolveMuscleUrl(any())).thenReturn(null);
        when(muscleAssetUseCase.findBaseAssets()).thenReturn(new MuscleBaseAssetsResult(null, null));

        MuscleIntensityResponse result =
                statsService.getMuscleIntensity(userId, LocalDate.of(1970, 1, 1), LocalDate.of(1970, 1, 1));

        assertThat(result.muscles())
                .containsExactly(
                        new MuscleIntensity(
                                weightedMuscleId,
                                "Weighted muscle",
                                "weighted-muscle",
                                BodyRegion.BACK,
                                null,
                                null,
                                10.0),
                        new MuscleIntensity(
                                highestMuscleId, "Highest muscle", "highest-muscle", BodyRegion.LEGS, null, null, 4.5),
                        new MuscleIntensity(
                                zeroMuscleId, "Zero muscle", "zero-muscle", BodyRegion.OTHER, null, null, 0.0));
    }

    @Test
    @DisplayName("Should round muscle intensity half up to one decimal")
    void getMuscleIntensity_shouldRoundHalfUpToOneDecimal() {
        UUID userId = UUID.randomUUID();
        UUID halfUpMuscleId = UUID.randomUUID();
        UUID lowerMuscleId = UUID.randomUUID();
        UUID maximumMuscleId = UUID.randomUUID();
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(UTC));
        when(statsRepository.getMuscleIntensity(userId, Instant.EPOCH, Instant.EPOCH.plusSeconds(86400)))
                .thenReturn(List.of(
                        new MuscleIntensityProjection(
                                halfUpMuscleId,
                                "Half-up muscle",
                                "half-up-muscle",
                                BodyRegion.ARMS,
                                null,
                                null,
                                ImpactLevel.PRIMARY,
                                0.105),
                        new MuscleIntensityProjection(
                                lowerMuscleId,
                                "Lower muscle",
                                "lower-muscle",
                                BodyRegion.ARMS,
                                null,
                                null,
                                ImpactLevel.PRIMARY,
                                0.101),
                        new MuscleIntensityProjection(
                                maximumMuscleId,
                                "Maximum muscle",
                                "maximum-muscle",
                                BodyRegion.LEGS,
                                null,
                                null,
                                ImpactLevel.PRIMARY,
                                1.0)));

        when(muscleAssetUseCase.resolveMuscleUrl(any())).thenReturn(null);
        when(muscleAssetUseCase.findBaseAssets()).thenReturn(new MuscleBaseAssetsResult(null, null));

        MuscleIntensityResponse result =
                statsService.getMuscleIntensity(userId, LocalDate.of(1970, 1, 1), LocalDate.of(1970, 1, 1));

        assertThat(result.muscles().getFirst().intensity()).isEqualTo(1.1);
        assertThat(result.muscles().get(1).intensity()).isEqualTo(1.0);
        assertThat(result.muscles().get(2).intensity()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("Should convert inclusive local dates to the user's timezone")
    void getMuscleIntensity_shouldConvertDatesInUserTimezone() {
        UUID userId = UUID.randomUUID();
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(SAO_PAULO));
        when(statsRepository.getMuscleIntensity(
                        userId, Instant.parse("2026-01-01T03:00:00Z"), Instant.parse("2026-01-02T03:00:00Z")))
                .thenReturn(List.of());

        when(muscleAssetUseCase.findBaseAssets()).thenReturn(new MuscleBaseAssetsResult(null, null));

        statsService.getMuscleIntensity(userId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));

        verify(statsRepository)
                .getMuscleIntensity(
                        userId, Instant.parse("2026-01-01T03:00:00Z"), Instant.parse("2026-01-02T03:00:00Z"));
    }

    @Test
    @DisplayName("Should return an empty muscle list when no muscle was trained")
    void getMuscleIntensity_shouldReturnEmptyMuscleListWhenNoProjectionExists() {
        UUID userId = UUID.randomUUID();
        when(getUserTimeZoneUseCase.getUserTimezoneById(userId)).thenReturn(Optional.of(UTC));
        when(statsRepository.getMuscleIntensity(userId, Instant.EPOCH, Instant.EPOCH.plusSeconds(86400)))
                .thenReturn(List.of());

        when(muscleAssetUseCase.findBaseAssets()).thenReturn(new MuscleBaseAssetsResult(null, null));

        MuscleIntensityResponse result =
                statsService.getMuscleIntensity(userId, LocalDate.of(1970, 1, 1), LocalDate.of(1970, 1, 1));

        assertThat(result.muscles()).isEmpty();
    }

    @Test
    @DisplayName("Should reject muscle intensity when start date is after end date")
    void getMuscleIntensity_shouldThrowWhenFromIsAfterTo() {
        assertThatThrownBy(() -> statsService.getMuscleIntensity(
                        UUID.randomUUID(), LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The start date cannot be after the end date");

        verifyNoInteractions(statsRepository, getUserTimeZoneUseCase);
    }
}
