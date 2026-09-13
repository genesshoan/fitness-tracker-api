package dev.genesshoan.fitnesstrackerapi.unit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.common.domain.ExerciseMetrics;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.mapper.ExerciseMetricsMapper;
import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.stats.domain.AchievementType;
import dev.genesshoan.fitnesstrackerapi.stats.dto.AchievementDTO;
import dev.genesshoan.fitnesstrackerapi.stats.dto.ExerciseProgressPointsDTO;
import dev.genesshoan.fitnesstrackerapi.stats.dto.OneRepMaxDTO;
import dev.genesshoan.fitnesstrackerapi.stats.dto.SessionVolumeDTO;
import dev.genesshoan.fitnesstrackerapi.stats.dto.StreakDTO;
import dev.genesshoan.fitnesstrackerapi.stats.mapper.AchievementMapper;
import dev.genesshoan.fitnesstrackerapi.stats.repository.StatsRepository;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.ExerciseProgressProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.OneRepMaxProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.RankedSetProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.VolumeSetProjection;
import dev.genesshoan.fitnesstrackerapi.stats.service.StatsService;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionSetBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.UserBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.WorkoutSessionBuilder;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import dev.genesshoan.fitnesstrackerapi.workout.repository.WorkoutSessionRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private StatsRepository statsRepository;

    @Mock
    private WorkoutSessionRepository workoutSessionRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private ExerciseMetricsMapper exerciseMetricsMapper;

    @Mock
    private AchievementMapper achievementMapper;

    @InjectMocks
    private StatsService statsService;

    @Test
    @DisplayName("Should calculate current streak in the user's timezone")
    void getCurrentStreak_shouldCalculateStreakInUserTimezone() {
        UUID userId = UUID.randomUUID();
        Instant reference = Instant.parse("2026-09-13T01:00:00Z");
        when(statsRepository.getTrainedDatesBeforeAsc(userId, reference))
                .thenReturn(List.of(Instant.parse("2026-09-11T23:00:00Z"), Instant.parse("2026-09-12T23:00:00Z")));

        StreakDTO result = statsService.getCurrentStreak(userId, reference, "America/Sao_Paulo");

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
        when(statsRepository.getTrainedDatesBeforeAsc(userId, reference)).thenReturn(List.of());

        StreakDTO result = statsService.getCurrentStreak(userId, reference, "UTC");

        assertThat(result).isEqualTo(new StreakDTO(0, 0, null));
    }

    @Test
    @DisplayName("Should reset current streak when the last workout is not recent")
    void getCurrentStreak_shouldResetCurrentStreakWhenLastDateIsOlderThanYesterday() {
        UUID userId = UUID.randomUUID();
        Instant reference = Instant.parse("2026-09-13T12:00:00Z");
        when(statsRepository.getTrainedDatesBeforeAsc(userId, reference))
                .thenReturn(List.of(
                        Instant.parse("2026-09-09T12:00:00Z"),
                        Instant.parse("2026-09-10T12:00:00Z"),
                        Instant.parse("2026-09-11T12:00:00Z")));

        StreakDTO result = statsService.getCurrentStreak(userId, reference, "UTC");

        assertThat(result.currentStreak()).isZero();
        assertThat(result.longestStreak()).isEqualTo(3);
        assertThat(result.lastActiveDay()).isEqualTo(LocalDate.of(2026, 9, 11));
    }

    @Test
    @DisplayName("Should calculate session volume when session belongs to user")
    void calculateSessionVolume_shouldReturnCalculatedVolume() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(workoutSessionRepository.existsByIdAndUserId(sessionId, userId)).thenReturn(true);
        when(statsRepository.getSetsForVolume(sessionId, userId))
                .thenReturn(List.of(new VolumeSetProjection(50.0, 10), new VolumeSetProjection(40.0, 5)));

        SessionVolumeDTO result = statsService.calculateSessionVolume(sessionId, userId);

        assertThat(result.volumeKg()).isEqualTo(700.0);
        verify(statsRepository).getSetsForVolume(sessionId, userId);
    }

    @Test
    @DisplayName("Should return empty exercise progress when no points exist")
    void getExerciseProgress_shouldReturnEmptyProgress() {
        UUID exerciseId = UUID.randomUUID();
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");
        when(exerciseRepository.existsById(exerciseId)).thenReturn(true);
        when(statsRepository.getExerciseProgressPoints(any(), any(), any(), any()))
                .thenReturn(List.of());

        assertThat(statsService.getExerciseProgress(UUID.randomUUID(), exerciseId, from, to, "UTC"))
                .isEqualTo(new ExerciseProgressPointsDTO(exerciseId, List.of()));
    }

    @Test
    @DisplayName("Should reject progress for an unknown exercise")
    void getExerciseProgress_shouldThrowWhenExerciseDoesNotExist() {
        UUID exerciseId = UUID.randomUUID();
        when(exerciseRepository.existsById(exerciseId)).thenReturn(false);

        assertThatThrownBy(() -> statsService.getExerciseProgress(
                        UUID.randomUUID(), exerciseId, Instant.now().minusSeconds(3600), Instant.now(), "UTC"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Exercise not found");

        verifyNoInteractions(statsRepository);
    }

    @Test
    @DisplayName("Should reject progress when start date is after end date")
    void getExerciseProgress_shouldThrowWhenFromIsAfterTo() {
        UUID exerciseId = UUID.randomUUID();
        Instant from = Instant.parse("2026-02-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-01T00:00:00Z");

        assertThatThrownBy(() -> statsService.getExerciseProgress(UUID.randomUUID(), exerciseId, from, to, "UTC"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Exercise progress not found");

        verifyNoInteractions(statsRepository, exerciseRepository);
    }

    @Test
    @DisplayName("Should reject session volume for an unknown session")
    void calculateSessionVolume_shouldThrowWhenSessionDoesNotBelongToUser() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(workoutSessionRepository.existsByIdAndUserId(sessionId, userId)).thenReturn(false);

        assertThatThrownBy(() -> statsService.calculateSessionVolume(sessionId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Workout session not found");

        verifyNoInteractions(statsRepository);
    }

    @Test
    @DisplayName("Should return the user's one rep max")
    void getOneRepMax_shouldReturnProjectionValue() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        when(exerciseRepository.existsById(exerciseId)).thenReturn(true);
        when(statsRepository.getSetForOneRepMax(userId, exerciseId))
                .thenReturn(Optional.of(new OneRepMaxProjection(100.0)));

        OneRepMaxDTO result = statsService.getOneRepMax(userId, exerciseId);

        assertThat(result).isEqualTo(new OneRepMaxDTO(exerciseId, 100.0));
    }

    @Test
    @DisplayName("Should return zero one rep max when no completed sets exist")
    void getOneRepMax_shouldReturnZeroWhenNoSetExists() {
        UUID exerciseId = UUID.randomUUID();
        when(exerciseRepository.existsById(exerciseId)).thenReturn(true);
        when(statsRepository.getSetForOneRepMax(any(), any())).thenReturn(Optional.empty());

        assertThat(statsService.getOneRepMax(UUID.randomUUID(), exerciseId).estimatedOneRepMax())
                .isZero();
    }

    @Test
    @DisplayName("Should reject one rep max for an unknown exercise")
    void getOneRepMax_shouldThrowWhenExerciseDoesNotExist() {
        UUID exerciseId = UUID.randomUUID();
        when(exerciseRepository.existsById(exerciseId)).thenReturn(false);

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
        when(exerciseRepository.existsById(exerciseId)).thenReturn(true);
        when(statsRepository.getExerciseProgressPoints(userId, exerciseId, from, to))
                .thenReturn(
                        List.of(new ExerciseProgressProjection(Instant.parse("2026-01-15T01:00:00Z"), 80.0, 5, 93.33)));

        ExerciseProgressPointsDTO result =
                statsService.getExerciseProgress(userId, exerciseId, from, to, "America/Sao_Paulo");

        assertThat(result.exerciseId()).isEqualTo(exerciseId);
        assertThat(result.progress()).hasSize(1);
        assertThat(result.progress().getFirst().date()).isEqualTo(LocalDate.of(2026, 1, 14));
        assertThat(result.progress().getFirst().weightKg()).isEqualTo(80.0);
    }

    @Test
    @DisplayName("Should return no achievements for an incomplete set")
    void calculateForSet_shouldReturnEmptyForIncompleteSet() {
        SessionSet set =
                SessionSetBuilder.aSessionSet(FAKER).withCompleted(false).build();

        assertThat(statsService.calculateForSet(set, UUID.randomUUID())).isEmpty();

        verifyNoInteractions(statsRepository, exerciseMetricsMapper, achievementMapper);
    }

    @Test
    @DisplayName("Should map achievements for a completed set")
    void calculateForSet_shouldMapCompletedSetAchievements() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        Exercise exercise = mock(Exercise.class);
        when(exercise.getId()).thenReturn(exerciseId);
        User user = UserBuilder.aUser(FAKER).withId(userId).build();
        WorkoutSession session =
                WorkoutSessionBuilder.aWorkoutSession(FAKER).forUser(user).build();
        var sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                .forExercise(exercise)
                .forWorkoutSession(session)
                .build();
        SessionSet set = SessionSetBuilder.aSessionSet(FAKER)
                .forSessionExercise(sessionExercise)
                .withCompleted(true)
                .build();
        List<AchievementDTO> expected = List.of();
        when(statsRepository.findRankedSets(any(), any(), any())).thenReturn(Map.of());
        when(exerciseMetricsMapper.toExerciseMetrics(set)).thenReturn(new ExerciseMetrics(10, 50.0, null, null));
        when(achievementMapper.toDtos(any())).thenReturn(expected);

        assertThat(statsService.calculateForSet(set, userId)).isEqualTo(expected);
        verify(achievementMapper).toDtos(any());
    }

    @Test
    @DisplayName("Should return mapped achievements for a new personal record")
    void calculateForSet_shouldReturnMappedAchievements() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        Exercise exercise = mock(Exercise.class);
        when(exercise.getId()).thenReturn(exerciseId);
        User user = UserBuilder.aUser(FAKER).withId(userId).build();
        WorkoutSession session =
                WorkoutSessionBuilder.aWorkoutSession(FAKER).forUser(user).build();
        var sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                .forExercise(exercise)
                .forWorkoutSession(session)
                .build();
        SessionSet set = SessionSetBuilder.aSessionSet(FAKER)
                .forSessionExercise(sessionExercise)
                .withCompleted(true)
                .build();
        AchievementDTO expectedAchievement = new AchievementDTO(AchievementType.NEW_MAX_WEIGHT, 50.0, null, null, null);
        when(statsRepository.findRankedSets(any(), any(), any())).thenReturn(Map.of());
        when(exerciseMetricsMapper.toExerciseMetrics(set)).thenReturn(new ExerciseMetrics(10, 50.0, null, null));
        when(achievementMapper.toDtos(any())).thenReturn(List.of(expectedAchievement));

        assertThat(statsService.calculateForSet(set, userId)).containsExactly(expectedAchievement);
    }

    @Test
    @DisplayName("Should return no achievements when the session has no completed sets")
    void calculateForSession_shouldReturnEmptyWhenNoCompletedSetsExist() {
        User user = UserBuilder.aUser(FAKER).build();
        WorkoutSession session =
                WorkoutSessionBuilder.aWorkoutSession(FAKER).forUser(user).build();

        assertThat(statsService.calculateForSession(session, user.getId())).isEmpty();

        verifyNoInteractions(statsRepository, exerciseMetricsMapper, achievementMapper);
    }

    @Test
    @DisplayName("Should calculate achievements for completed sets in a session")
    void calculateForSession_shouldReturnAchievementsBySetId() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        Exercise exercise = mock(Exercise.class);
        when(exercise.getId()).thenReturn(exerciseId);
        User user = UserBuilder.aUser(FAKER).withId(userId).build();
        WorkoutSession session =
                WorkoutSessionBuilder.aWorkoutSession(FAKER).forUser(user).build();
        var sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                .forExercise(exercise)
                .forWorkoutSession(session)
                .build();
        SessionSet completedSet = SessionSetBuilder.aSessionSet(FAKER)
                .forSessionExercise(sessionExercise)
                .withCompleted(true)
                .build();
        SessionSet incompleteSet = SessionSetBuilder.aSessionSet(FAKER)
                .forSessionExercise(sessionExercise)
                .withSetNumber(2)
                .withCompleted(false)
                .build();
        sessionExercise.addSet(completedSet);
        sessionExercise.addSet(incompleteSet);
        session.addExerciseAt(sessionExercise, 1);
        AchievementDTO achievement = new AchievementDTO(AchievementType.NEW_MAX_WEIGHT, 50.0, null, null, null);

        when(statsRepository.findRankedSets(userId, java.util.Set.of(exerciseId), session.getStartedAt()))
                .thenReturn(Map.of());
        when(exerciseMetricsMapper.toExerciseMetrics(completedSet))
                .thenReturn(new ExerciseMetrics(10, 50.0, null, null));
        when(achievementMapper.toDtos(any())).thenReturn(List.of(achievement));

        assertThat(statsService.calculateForSession(session, userId))
                .containsEntry(completedSet.getId(), List.of(achievement));
        verify(exerciseMetricsMapper).toExerciseMetrics(completedSet);
    }

    @Test
    @DisplayName("Should use existing records when calculating session achievements")
    void calculateForSession_shouldCompareAgainstExistingRecords() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        UUID previousSetId = UUID.randomUUID();
        UUID previousSessionId = UUID.randomUUID();
        Exercise exercise = mock(Exercise.class);
        when(exercise.getId()).thenReturn(exerciseId);
        User user = UserBuilder.aUser(FAKER).withId(userId).build();
        WorkoutSession session =
                WorkoutSessionBuilder.aWorkoutSession(FAKER).forUser(user).build();
        var sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                .forExercise(exercise)
                .forWorkoutSession(session)
                .build();
        SessionSet set = SessionSetBuilder.aSessionSet(FAKER)
                .forSessionExercise(sessionExercise)
                .withCompleted(true)
                .build();
        sessionExercise.addSet(set);
        session.addExerciseAt(sessionExercise, 1);
        AchievementDTO achievement =
                new AchievementDTO(AchievementType.NEW_MAX_WEIGHT, 60.0, 50.0, previousSetId, previousSessionId);
        when(statsRepository.findRankedSets(any(), any(), any()))
                .thenReturn(Map.of(
                        exerciseId,
                        List.of(new RankedSetProjection(
                                previousSetId, previousSessionId, exerciseId, 50.0, 10, 5.0, 30, 1, 1, 1, 1, 1))));
        when(exerciseMetricsMapper.toExerciseMetrics(set)).thenReturn(new ExerciseMetrics(12, 60.0, 40, 6.0));
        when(achievementMapper.toDtos(any())).thenReturn(List.of(achievement));

        assertThat(statsService.calculateForSession(session, userId)).containsEntry(set.getId(), List.of(achievement));
    }
}
