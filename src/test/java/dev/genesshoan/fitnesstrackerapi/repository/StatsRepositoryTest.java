package dev.genesshoan.fitnesstrackerapi.repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.BodyRegion;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.Muscle;
import dev.genesshoan.fitnesstrackerapi.stats.repository.StatsRepository;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.MonthlyVolumeProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.MuscleIntensityProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.VolumeSetProjection;
import dev.genesshoan.fitnesstrackerapi.testdata.TestEntityFactory;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.MuscleBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionSetBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.WorkoutSessionBuilder;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionStatus;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class StatsRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private StatsRepository statsRepository;

    @Autowired
    private TestEntityFactory testEntityFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Should find ranked completed sets before a session")
    void findRankedSets_shouldGroupTopSetsByExercise() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise =
                testEntityFactory.createAndPersistExercise(ExerciseBuilder.anExercise(testEntityFactory.faker()));
        Instant startedAt = Instant.parse("2026-09-13T10:00:00Z");
        WorkoutSession session = persistCompletedSession(user, startedAt, startedAt.plusSeconds(3600));
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withWeightKg(100.0)
                .withReps(5)
                .withCompleted(true));

        var result = statsRepository.findRankedSets(user.getId(), Set.of(exercise.getId()), startedAt.plusSeconds(1));

        assertThat(result).containsKey(exercise.getId());
        assertThat(result.get(exercise.getId())).anySatisfy(set -> {
            assertThat(set.weightKg()).isEqualTo(100.0);
            assertThat(set.reps()).isEqualTo(5);
            assertThat(set.rnWeight()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("Should return no ranked sets when no exercises are requested")
    void findRankedSets_shouldReturnEmptyForNoExerciseIds() {
        assertThat(statsRepository.findRankedSets(UUID.randomUUID(), Set.of(), Instant.now()))
                .isEmpty();
    }

    @Test
    @DisplayName("Should not return ranked sets for another user")
    void findRankedSets_shouldFilterByUser() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        Instant startedAt = Instant.parse("2026-09-13T10:00:00Z");
        WorkoutSession session = persistCompletedSession(owner, startedAt, startedAt.plusSeconds(3600));
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withCompleted(true));

        assertThat(statsRepository.findRankedSets(
                        otherUser.getId(), Set.of(exercise.getId()), startedAt.plusSeconds(1)))
                .isEmpty();
    }

    @Test
    @DisplayName("Should return trained dates in ascending order")
    void getTrainedDatesBeforeAsc_shouldReturnOrderedDistinctDates() {
        User user = testEntityFactory.createAndPersistUser();
        Instant first = Instant.parse("2026-09-10T10:00:00Z");
        persistCompletedSession(user, first, first);
        persistCompletedSession(user, first, first);
        Instant second = Instant.parse("2026-09-12T10:00:00Z");
        persistCompletedSession(user, second, second);

        assertThat(statsRepository.getTrainedDatesBeforeAsc(user.getId(), Instant.parse("2026-09-13T00:00:00Z")))
                .containsExactly(first, second);
    }

    @Test
    @DisplayName("Should exclude trained dates at or after the reference instant")
    void getTrainedDatesBeforeAsc_shouldRespectReferenceInstant() {
        User user = testEntityFactory.createAndPersistUser();
        Instant included = Instant.parse("2026-09-10T10:00:00Z");
        Instant excluded = Instant.parse("2026-09-12T10:00:00Z");
        persistCompletedSession(user, included, included);
        persistCompletedSession(user, excluded, excluded);

        assertThat(statsRepository.getTrainedDatesBeforeAsc(user.getId(), excluded))
                .containsExactly(included);
    }

    @Test
    @DisplayName("Should return no trained dates for another user")
    void getTrainedDatesBeforeAsc_shouldFilterByUser() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        Instant completedAt = Instant.parse("2026-09-10T10:00:00Z");
        persistCompletedSession(owner, completedAt, completedAt);

        assertThat(statsRepository.getTrainedDatesBeforeAsc(otherUser.getId(), completedAt.plusSeconds(1)))
                .isEmpty();
    }

    @Test
    @DisplayName("Should return only completed weighted sets for session volume")
    void getSetsForVolume_shouldReturnCompletedWeightedSets() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        WorkoutSession session = persistCompletedSession(user, Instant.now().minusSeconds(100), Instant.now());
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withWeightKg(50.0)
                .withReps(10)
                .withCompleted(true));
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(2)
                .withWeightKg(null)
                .withCompleted(true));

        assertThat(statsRepository.getSetsForVolume(session.getId(), user.getId()))
                .containsExactly(new VolumeSetProjection(50.0, 10));
    }

    @Test
    @DisplayName("Should return no volume sets for another user")
    void getSetsForVolume_shouldFilterByUser() {
        User owner = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        WorkoutSession session = persistCompletedSession(owner, Instant.now().minusSeconds(100), Instant.now());
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withCompleted(true));

        assertThat(statsRepository.getSetsForVolume(session.getId(), otherUser.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("Should aggregate completed volume by local calendar month")
    void getMonthlyVolume_shouldAggregateByMonthAndTimezone() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        Instant completedAt = Instant.parse("2026-01-31T23:30:00Z");
        WorkoutSession session = persistCompletedSession(user, completedAt.minusSeconds(100), completedAt);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withCompleted(true)
                .withReps(10)
                .withWeightKg(50.0));

        assertThat(statsRepository.getMonthlyVolume(
                        user.getId(),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-03-01T00:00:00Z"),
                        "UTC"))
                .containsExactly(new MonthlyVolumeProjection("2026-01", 500.0));
        assertThat(statsRepository.getMonthlyVolume(
                        user.getId(),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-03-01T00:00:00Z"),
                        "Europe/Berlin"))
                .containsExactly(new MonthlyVolumeProjection("2026-02", 500.0));
    }

    @Test
    @DisplayName("Should return total completed volume for an instant range")
    void getVolume_shouldAggregateCompletedSetsInRange() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        Instant completedAt = Instant.parse("2026-01-15T10:00:00Z");
        WorkoutSession session = persistCompletedSession(user, completedAt.minusSeconds(100), completedAt);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        persistSet(sessionExercise, 1, 8, 75.0, null, null, true);

        assertThat(statsRepository.getVolume(
                        user.getId(), Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-02-01T00:00:00Z")))
                .isEqualTo(600.0);
    }

    @Test
    @DisplayName("Should return the highest estimated one rep max")
    void getSetForOneRepMax_shouldReturnHighestValue() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        WorkoutSession session = persistCompletedSession(user, Instant.now().minusSeconds(100), Instant.now());
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withWeightKg(80.0)
                .withReps(5)
                .withCompleted(true));
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(2)
                .withWeightKg(100.0)
                .withReps(5)
                .withCompleted(true));

        assertThat(statsRepository.getSetForOneRepMax(user.getId(), exercise.getId()))
                .hasValueSatisfying(
                        result -> assertThat(result.estimatedOneRepMax()).isEqualTo(116.66666666666667));
    }

    @Test
    @DisplayName("Should return empty one rep max when no qualifying sets exist")
    void getSetForOneRepMax_shouldReturnEmptyWhenNoSetsExist() {
        assertThat(statsRepository.getSetForOneRepMax(UUID.randomUUID(), UUID.randomUUID()))
                .isEmpty();
    }

    @Test
    @DisplayName("Should return the strongest set from each session in progress range")
    void getExerciseProgressPoints_shouldReturnBestSetPerSession() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        Instant completedAt = Instant.parse("2026-09-10T10:00:00Z");
        WorkoutSession session = persistCompletedSession(user, completedAt.minusSeconds(3600), completedAt);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withWeightKg(80.0)
                .withReps(5)
                .withCompleted(true));
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(2)
                .withWeightKg(100.0)
                .withReps(5)
                .withCompleted(true));

        var result = statsRepository.getExerciseProgressPoints(
                user.getId(), exercise.getId(), completedAt.minusSeconds(1), completedAt.plusSeconds(1));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().weightKg()).isEqualTo(100.0);
        assertThat(result.getFirst().reps()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should return no progress points outside the requested range")
    void getExerciseProgressPoints_shouldRespectDateRange() {
        User user = testEntityFactory.createAndPersistUser();
        Exercise exercise = testEntityFactory.createAndPersistExercise();
        Instant completedAt = Instant.parse("2026-09-10T10:00:00Z");
        WorkoutSession session = persistCompletedSession(user, completedAt.minusSeconds(3600), completedAt);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withCompleted(true));

        assertThat(statsRepository.getExerciseProgressPoints(
                        user.getId(), exercise.getId(), completedAt.plusSeconds(1), completedAt.plusSeconds(2)))
                .isEmpty();
    }

    @Test
    @DisplayName("Should return every muscle with zero stimulus when no data qualifies")
    void getMuscleIntensity_shouldReturnZeroForEveryMuscleWhenNoDataQualifies() {
        Muscle unstimulatedMuscle =
                testEntityFactory.createAndPersistMuscle(MuscleBuilder.aMuscle(testEntityFactory.faker())
                        .withName("Unstimulated Muscle")
                        .withSlug("unstimulated-muscle")
                        .withBodyRegion(BodyRegion.OTHER));
        Instant from = Instant.parse("2026-01-01T00:00:00Z");

        List<MuscleIntensityProjection> result =
                statsRepository.getMuscleIntensity(UUID.randomUUID(), from, from.plusSeconds(86400));

        assertThat(result).isNotEmpty().allSatisfy(projection -> {
            assertThat(projection.impactLevel()).isNull();
            assertThat(projection.rawStimulus()).isZero();
        });
        assertThat(result)
                .contains(new MuscleIntensityProjection(
                        unstimulatedMuscle.getId(),
                        "Unstimulated Muscle",
                        "unstimulated-muscle",
                        BodyRegion.OTHER,
                        null,
                        0.0));
    }

    @Test
    @DisplayName("Should aggregate strength stimulus by impact level with safe load handling")
    void getMuscleIntensity_shouldAggregateStrengthStimulusByImpactLevel() {
        Muscle targetMuscle = testEntityFactory.createAndPersistMuscle(MuscleBuilder.aMuscle(testEntityFactory.faker())
                .withName("Strength Muscle")
                .withSlug("strength-muscle")
                .withBodyRegion(BodyRegion.CHEST));
        Muscle zeroMuscle = testEntityFactory.createAndPersistMuscle(MuscleBuilder.aMuscle(testEntityFactory.faker())
                .withName("Zero Strength Muscle")
                .withSlug("zero-strength-muscle")
                .withBodyRegion(BodyRegion.BACK));
        Exercise primaryExercise = persistExercise(Category.STRENGTH, targetMuscle, ImpactLevel.PRIMARY);
        Exercise secondaryExercise = persistExercise(Category.STRENGTH, targetMuscle, ImpactLevel.SECONDARY);
        User user = testEntityFactory.createAndPersistUser();
        Instant completedAt = Instant.parse("2026-09-10T10:00:00Z");
        WorkoutSession session = persistCompletedSession(user, completedAt.minusSeconds(3600), completedAt);

        SessionExercise primarySessionExercise = persistSessionExercise(session, primaryExercise, 1);
        persistSet(primarySessionExercise, 1, 1000, 1000.0, null, null, true);

        SessionExercise secondarySessionExercise = persistSessionExercise(session, secondaryExercise, 2);
        persistSet(secondarySessionExercise, 1, 5, null, null, null, true);
        persistSet(secondarySessionExercise, 2, 8, 0.0, null, null, true);

        List<MuscleIntensityProjection> result = statsRepository.getMuscleIntensity(
                user.getId(), completedAt.minusSeconds(1), completedAt.plusSeconds(1));

        List<MuscleIntensityProjection> targetRows = result.stream()
                .filter(projection -> projection.muscleId().equals(targetMuscle.getId()))
                .toList();
        assertThat(targetRows)
                .extracting(MuscleIntensityProjection::impactLevel)
                .containsExactlyInAnyOrder(ImpactLevel.PRIMARY, ImpactLevel.SECONDARY);
        assertThat(targetRows)
                .filteredOn(projection -> projection.impactLevel() == ImpactLevel.PRIMARY)
                .singleElement()
                .extracting(MuscleIntensityProjection::rawStimulus)
                .satisfies(stimulus -> assertThat(stimulus).isCloseTo(1.0 + Math.log(1001.0), offset(1e-12)));
        assertThat(targetRows)
                .filteredOn(projection -> projection.impactLevel() == ImpactLevel.SECONDARY)
                .singleElement()
                .extracting(MuscleIntensityProjection::rawStimulus)
                .satisfies(stimulus ->
                        assertThat(stimulus).isCloseTo((1.0 + Math.log(6.0)) + (1.0 + Math.log(9.0)), offset(1e-12)));
        assertThat(result)
                .contains(new MuscleIntensityProjection(
                        zeroMuscle.getId(),
                        "Zero Strength Muscle",
                        "zero-strength-muscle",
                        BodyRegion.BACK,
                        null,
                        0.0));
    }

    @Test
    @DisplayName("Should keep very high strength volume finite and stronger than low volume")
    void getMuscleIntensity_shouldBoundVeryHighStrengthVolume() {
        Muscle highVolumeMuscle =
                testEntityFactory.createAndPersistMuscle(MuscleBuilder.aMuscle(testEntityFactory.faker())
                        .withName("High Volume Muscle")
                        .withSlug("high-volume-muscle")
                        .withBodyRegion(BodyRegion.LEGS));
        Muscle lowVolumeMuscle =
                testEntityFactory.createAndPersistMuscle(MuscleBuilder.aMuscle(testEntityFactory.faker())
                        .withName("Low Volume Muscle")
                        .withSlug("low-volume-muscle")
                        .withBodyRegion(BodyRegion.BACK));
        Exercise highVolumeExercise = persistExercise(Category.STRENGTH, highVolumeMuscle, ImpactLevel.PRIMARY);
        Exercise lowVolumeExercise = persistExercise(Category.STRENGTH, lowVolumeMuscle, ImpactLevel.PRIMARY);
        User user = testEntityFactory.createAndPersistUser();
        Instant completedAt = Instant.parse("2026-09-10T10:00:00Z");
        WorkoutSession session = persistCompletedSession(user, completedAt.minusSeconds(3600), completedAt);

        persistSet(persistSessionExercise(session, highVolumeExercise, 1), 1, 1_000_000, 1_000_000.0, null, null, true);
        persistSet(persistSessionExercise(session, lowVolumeExercise, 2), 1, 1, 1.0, null, null, true);

        List<MuscleIntensityProjection> result = statsRepository.getMuscleIntensity(
                user.getId(), completedAt.minusSeconds(1), completedAt.plusSeconds(1));
        double highVolumeStimulus = result.stream()
                .filter(projection -> projection.muscleId().equals(highVolumeMuscle.getId()))
                .findFirst()
                .orElseThrow()
                .rawStimulus();
        double lowVolumeStimulus = result.stream()
                .filter(projection -> projection.muscleId().equals(lowVolumeMuscle.getId()))
                .findFirst()
                .orElseThrow()
                .rawStimulus();

        assertThat(highVolumeStimulus).isFinite().isGreaterThan(lowVolumeStimulus);
        assertThat(lowVolumeStimulus).isFinite().isPositive();
    }

    @Test
    @DisplayName("Should use normalized cardio duration and distance without unit conversion")
    void getMuscleIntensity_shouldNormalizeCardioDurationAndDistanceIndependently() {
        Muscle muscle = testEntityFactory.createAndPersistMuscle(MuscleBuilder.aMuscle(testEntityFactory.faker())
                .withName("Cardio Muscle")
                .withSlug("cardio-muscle")
                .withBodyRegion(BodyRegion.LEGS));
        Exercise exercise = persistExercise(Category.CARDIO, muscle, ImpactLevel.PRIMARY);
        User user = testEntityFactory.createAndPersistUser();
        Instant completedAt = Instant.parse("2026-09-10T10:00:00Z");
        WorkoutSession session = persistCompletedSession(user, completedAt.minusSeconds(3600), completedAt);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);

        persistSet(sessionExercise, 1, null, null, 1000, null, true);
        persistSet(sessionExercise, 2, null, null, 100, 9.0, true);
        persistSet(sessionExercise, 3, null, null, 100, 10.0, true);
        persistSet(sessionExercise, 4, null, null, null, null, true);

        List<MuscleIntensityProjection> result = statsRepository.getMuscleIntensity(
                user.getId(), completedAt.minusSeconds(1), completedAt.plusSeconds(1));

        double durationFactor = (1.0 + Math.log(101.0)) / (1.0 + Math.log(1001.0));
        double distanceFactor = (1.0 + Math.log(10.0)) / (1.0 + Math.log(11.0));
        double expectedStimulus = 1.0 + Math.max(durationFactor, distanceFactor) + 1.0 + 1.0;
        assertThat(result)
                .filteredOn(projection -> projection.muscleId().equals(muscle.getId()))
                .singleElement()
                .extracting(MuscleIntensityProjection::rawStimulus)
                .satisfies(stimulus -> assertThat(stimulus).isCloseTo(expectedStimulus, offset(1e-12)));
    }

    @Test
    @DisplayName("Should scale mobility stimulus by normalized duration only")
    void getMuscleIntensity_shouldNormalizeMobilityDuration() {
        Muscle muscle = testEntityFactory.createAndPersistMuscle(MuscleBuilder.aMuscle(testEntityFactory.faker())
                .withName("Mobility Muscle")
                .withSlug("mobility-muscle")
                .withBodyRegion(BodyRegion.OTHER));
        Exercise exercise = persistExercise(Category.MOBILITY, muscle, ImpactLevel.PRIMARY);
        User user = testEntityFactory.createAndPersistUser();
        Instant completedAt = Instant.parse("2026-09-10T10:00:00Z");
        WorkoutSession session = persistCompletedSession(user, completedAt.minusSeconds(3600), completedAt);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);

        // MOBILITY guarantees duration only: reps, weight and distance are always null.
        persistSet(sessionExercise, 1, null, null, 100, null, true);
        persistSet(sessionExercise, 2, null, null, 25, null, true);

        List<MuscleIntensityProjection> result = statsRepository.getMuscleIntensity(
                user.getId(), completedAt.minusSeconds(1), completedAt.plusSeconds(1));

        double expectedStimulus = 1.0 + (1.0 + Math.log(26.0)) / (1.0 + Math.log(101.0));
        assertThat(result)
                .filteredOn(projection -> projection.muscleId().equals(muscle.getId()))
                .singleElement()
                .extracting(MuscleIntensityProjection::rawStimulus)
                .satisfies(stimulus ->
                        assertThat(stimulus).isFinite().isPositive().isCloseTo(expectedStimulus, offset(1e-12)));
    }

    @Test
    @DisplayName("Should contribute zero stimulus for mobility sets without a positive duration")
    void getMuscleIntensity_shouldContributeZeroForMobilitySetWithoutDuration() {
        Muscle muscle = testEntityFactory.createAndPersistMuscle(MuscleBuilder.aMuscle(testEntityFactory.faker())
                .withName("Timeless Mobility Muscle")
                .withSlug("timeless-mobility-muscle")
                .withBodyRegion(BodyRegion.OTHER));
        Exercise exercise = persistExercise(Category.MOBILITY, muscle, ImpactLevel.PRIMARY);
        User user = testEntityFactory.createAndPersistUser();
        Instant completedAt = Instant.parse("2026-09-10T10:00:00Z");
        WorkoutSession session = persistCompletedSession(user, completedAt.minusSeconds(3600), completedAt);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);

        persistSet(sessionExercise, 1, null, null, null, null, true);
        persistSet(sessionExercise, 2, null, null, 0, null, true);

        List<MuscleIntensityProjection> result = statsRepository.getMuscleIntensity(
                user.getId(), completedAt.minusSeconds(1), completedAt.plusSeconds(1));

        assertThat(result)
                .filteredOn(projection -> projection.muscleId().equals(muscle.getId()))
                .singleElement()
                .extracting(MuscleIntensityProjection::rawStimulus)
                .satisfies(stimulus -> assertThat(stimulus).isZero().isFinite());
    }

    @Test
    @Transactional
    @DisplayName("Should contribute zero stimulus for a category outside the known modalities")
    void getMuscleIntensity_shouldIgnoreSetsOutsideKnownModalities() {
        Muscle muscle = testEntityFactory.createAndPersistMuscle(MuscleBuilder.aMuscle(testEntityFactory.faker())
                .withName("Outsider Muscle")
                .withSlug("outsider-muscle")
                .withBodyRegion(BodyRegion.OTHER));
        Exercise exercise = persistExercise(Category.STRENGTH, muscle, ImpactLevel.PRIMARY);
        User user = testEntityFactory.createAndPersistUser();
        Instant completedAt = Instant.parse("2026-09-10T10:00:00Z");
        WorkoutSession session = persistCompletedSession(user, completedAt.minusSeconds(3600), completedAt);
        SessionExercise sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);

        persistSet(sessionExercise, 1, 10, 50.0, null, null, true);

        // The production schema only permits the three known modalities, so an out-of-domain
        // category can exist only by relaxing the constraint inside this rolled-back
        // transaction. Both names are dropped because the Hibernate test schema and the
        // Flyway migration name the same check differently.
        jdbcTemplate.execute("ALTER TABLE exercises DROP CONSTRAINT IF EXISTS ck_exercises_category");
        jdbcTemplate.execute("ALTER TABLE exercises DROP CONSTRAINT IF EXISTS exercises_category_check");
        jdbcTemplate.update("UPDATE exercises SET category = 'CALISTHENICS' WHERE id = ?", exercise.getId());

        // The category flip is a raw UPDATE that can silently match no rows, so prove the
        // premise: without this the test could pass without ever reaching the query's
        // unknown-category branch.
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT category FROM exercises WHERE id = ?", String.class, exercise.getId()))
                .isEqualTo("CALISTHENICS");

        List<MuscleIntensityProjection> result = statsRepository.getMuscleIntensity(
                user.getId(), completedAt.minusSeconds(1), completedAt.plusSeconds(1));

        assertThat(result)
                .filteredOn(projection -> projection.muscleId().equals(muscle.getId()))
                .singleElement()
                .extracting(MuscleIntensityProjection::rawStimulus)
                .satisfies(stimulus -> assertThat(stimulus).isZero().isFinite());
    }

    @Test
    @DisplayName("Should filter muscle intensity by ownership, completion, and date boundaries")
    void getMuscleIntensity_shouldApplyAllQualifyingFilters() {
        Muscle muscle = testEntityFactory.createAndPersistMuscle(MuscleBuilder.aMuscle(testEntityFactory.faker())
                .withName("Filtered Muscle")
                .withSlug("filtered-muscle")
                .withBodyRegion(BodyRegion.BACK));
        Exercise exercise = persistExercise(Category.STRENGTH, muscle, ImpactLevel.PRIMARY);
        User user = testEntityFactory.createAndPersistUser();
        User otherUser = testEntityFactory.createAndPersistUser();
        Instant from = Instant.parse("2026-09-10T00:00:00Z");
        Instant toExclusive = from.plusSeconds(86400);

        WorkoutSession includedSession = persistCompletedSession(user, from.minusSeconds(3600), from);
        persistSet(
                testEntityFactory.createAndPersistSessionExercise(includedSession, exercise),
                1,
                10,
                10.0,
                null,
                null,
                true);

        WorkoutSession upperBoundarySession = persistCompletedSession(user, from, toExclusive);
        persistSet(
                testEntityFactory.createAndPersistSessionExercise(upperBoundarySession, exercise),
                1,
                100000,
                100000.0,
                null,
                null,
                true);

        WorkoutSession incompleteSetSession = persistCompletedSession(user, from, from.plusSeconds(3600));
        persistSet(
                testEntityFactory.createAndPersistSessionExercise(incompleteSetSession, exercise),
                1,
                100000,
                100000.0,
                null,
                null,
                false);

        WorkoutSession inProgressSession =
                persistSession(user, SessionStatus.IN_PROGRESS, from.minusSeconds(3600), from.plusSeconds(7200));
        persistSet(
                testEntityFactory.createAndPersistSessionExercise(inProgressSession, exercise),
                1,
                100000,
                100000.0,
                null,
                null,
                true);

        WorkoutSession missingCompletionTimeSession = persistCompletedSession(user, from.minusSeconds(3600), null);
        persistSet(
                testEntityFactory.createAndPersistSessionExercise(missingCompletionTimeSession, exercise),
                1,
                100000,
                100000.0,
                null,
                null,
                true);

        WorkoutSession otherUserSession =
                persistCompletedSession(otherUser, from.minusSeconds(3600), from.plusSeconds(60));
        persistSet(
                testEntityFactory.createAndPersistSessionExercise(otherUserSession, exercise),
                1,
                100000,
                100000.0,
                null,
                null,
                true);

        List<MuscleIntensityProjection> result = statsRepository.getMuscleIntensity(user.getId(), from, toExclusive);

        assertThat(result)
                .filteredOn(projection -> projection.muscleId().equals(muscle.getId()))
                .singleElement()
                .extracting(MuscleIntensityProjection::rawStimulus)
                .satisfies(stimulus -> assertThat(stimulus).isCloseTo(1.0 + Math.log(11.0), offset(1e-12)));
    }

    private SessionExercise persistSessionExercise(WorkoutSession session, Exercise exercise, int position) {
        return testEntityFactory.createAndPersistSessionExercise(
                SessionExerciseBuilder.aSessionExercise(testEntityFactory.faker())
                        .forWorkoutSession(session)
                        .forExercise(exercise)
                        .withPosition(position));
    }

    private Exercise persistExercise(Category category, Muscle muscle, ImpactLevel impactLevel) {
        return testEntityFactory.createAndPersistExerciseWithMuscles(
                ExerciseBuilder.anExercise(testEntityFactory.faker()).withCategory(category),
                List.of(muscle),
                impactLevel);
    }

    private SessionSet persistSet(
            SessionExercise sessionExercise,
            int setNumber,
            Integer reps,
            Double weightKg,
            Integer durationSeconds,
            Double distanceKm,
            boolean completed) {
        return testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(setNumber)
                .withReps(reps)
                .withWeightKg(weightKg)
                .withDurationSeconds(durationSeconds)
                .withDistanceKm(distanceKm)
                .withCompleted(completed));
    }

    private WorkoutSession persistSession(User user, SessionStatus status, Instant startedAt, Instant completedAt) {
        return testEntityFactory.createAndPersistWorkoutSession(
                WorkoutSessionBuilder.aWorkoutSession(testEntityFactory.faker())
                        .forUser(user)
                        .withStatus(status)
                        .withStartedAt(startedAt)
                        .withCompletedAt(completedAt));
    }

    private WorkoutSession persistCompletedSession(User user, Instant startedAt, Instant completedAt) {
        return persistSession(user, SessionStatus.COMPLETED, startedAt, completedAt);
    }
}
