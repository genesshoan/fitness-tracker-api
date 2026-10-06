package dev.genesshoan.fitnesstrackerapi.stats.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.BodyRegion;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle.Muscle;
import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.MuscleBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionSetBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.UserBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.WorkoutSessionBuilder;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionStatus;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("Integration Tests - Stats Controller")
class StatsControllerIT extends AbstractIntegrationTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = testEntityFactory.createAndPersistUser(
                UserBuilder.aUser(testEntityFactory.faker()).withTimezone("UTC"));
    }

    private RequestPostProcessor asUser(User targetUser) {
        UserDetails userDetails = new UserDetailsImpl(targetUser);
        return SecurityMockMvcRequestPostProcessors.user(userDetails);
    }

    @Test
    @DisplayName("Should return 200 with progress data for authenticated user")
    void getExerciseProgress_shouldReturn200WithData() throws Exception {
        var exercise = testEntityFactory.createAndPersistExercise();
        Instant completedAt = Instant.parse("2026-01-15T10:00:00Z");

        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(
                WorkoutSessionBuilder.aWorkoutSession(testEntityFactory.faker())
                        .forUser(user)
                        .withStatus(SessionStatus.COMPLETED)
                        .withCompletedAt(completedAt)
                        .withStartedAt(completedAt.minusSeconds(3600)));

        var sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(1)
                .withCompleted(true)
                .withReps(8)
                .withWeightKg(80.0));

        mockMvc.perform(get("/api/v1/stats/progress")
                        .param("exerciseId", exercise.getId().toString())
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-02-01T00:00:00Z")
                        .with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exerciseId").value(exercise.getId().toString()))
                .andExpect(jsonPath("$.progress.length()").value(1));
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated")
    void getExerciseProgress_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/stats/progress")
                        .param("exerciseId", UUID.randomUUID().toString())
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-02-01T00:00:00Z"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 400 when progress parameters are invalid")
    void getExerciseProgress_shouldReturn400WhenParametersAreInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/stats/progress")
                        .param("exerciseId", "not-a-uuid")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-02-01T00:00:00Z")
                        .with(asUser(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when the progress range is inverted")
    void getExerciseProgress_shouldReturn400WhenRangeIsInverted() throws Exception {
        mockMvc.perform(get("/api/v1/stats/progress")
                        .param("exerciseId", UUID.randomUUID().toString())
                        .param("from", "2026-02-01T00:00:00Z")
                        .param("to", "2026-01-01T00:00:00Z")
                        .with(asUser(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 when requesting another user's session volume")
    void getSessionVolume_shouldReturn404WhenSessionBelongsToAnotherUser() throws Exception {
        User otherUser = testEntityFactory.createAndPersistUser();
        WorkoutSession otherSession = testEntityFactory.createAndPersistWorkoutSession(otherUser);

        mockMvc.perform(get("/api/v1/stats/volume")
                        .param("sessionId", otherSession.getId().toString())
                        .with(asUser(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return monthly volume in the user's timezone")
    void getMonthlyVolume_shouldAggregateCompletedSetsByMonth() throws Exception {
        var exercise = testEntityFactory.createAndPersistExercise();
        Instant january = Instant.parse("2026-01-31T23:30:00Z");
        Instant february = Instant.parse("2026-02-01T00:30:00Z");

        WorkoutSession januarySession = testEntityFactory.createAndPersistWorkoutSession(
                WorkoutSessionBuilder.aWorkoutSession(testEntityFactory.faker())
                        .forUser(user)
                        .withStatus(SessionStatus.COMPLETED)
                        .withStartedAt(january.minusSeconds(3600))
                        .withCompletedAt(january));
        var januaryExercise = testEntityFactory.createAndPersistSessionExercise(januarySession, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(januaryExercise)
                .withCompleted(true)
                .withReps(10)
                .withWeightKg(50.0));

        WorkoutSession februarySession = testEntityFactory.createAndPersistWorkoutSession(
                WorkoutSessionBuilder.aWorkoutSession(testEntityFactory.faker())
                        .forUser(user)
                        .withStatus(SessionStatus.COMPLETED)
                        .withStartedAt(february.minusSeconds(3600))
                        .withCompletedAt(february));
        var februaryExercise = testEntityFactory.createAndPersistSessionExercise(februarySession, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(februaryExercise)
                .withCompleted(true)
                .withReps(5)
                .withWeightKg(40.0));

        mockMvc.perform(get("/api/v1/stats/volume/monthly")
                        .param("from", "2026-01-01")
                        .param("to", "2026-02-28")
                        .with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value("2026-01"))
                .andExpect(jsonPath("$[0].volumeKg").value(500.0))
                .andExpect(jsonPath("$[1].month").value("2026-02"))
                .andExpect(jsonPath("$[1].volumeKg").value(200.0));
    }

    @Test
    @DisplayName("Should return 400 when monthly volume range is inverted")
    void getMonthlyVolume_shouldReturn400WhenRangeIsInverted() throws Exception {
        mockMvc.perform(get("/api/v1/stats/volume/monthly")
                        .param("from", "2026-02-01")
                        .param("to", "2026-01-01")
                        .with(asUser(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 for unauthenticated monthly volume requests")
    void getMonthlyVolume_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/stats/volume/monthly")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return total volume for an inclusive date range")
    void getVolume_shouldAggregateCompletedSetsForDateRange() throws Exception {
        var exercise = testEntityFactory.createAndPersistExercise();
        Instant completedAt = Instant.parse("2026-01-15T10:00:00Z");
        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(
                WorkoutSessionBuilder.aWorkoutSession(testEntityFactory.faker())
                        .forUser(user)
                        .withStatus(SessionStatus.COMPLETED)
                        .withStartedAt(completedAt.minusSeconds(3600))
                        .withCompletedAt(completedAt));
        var sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withCompleted(true)
                .withReps(8)
                .withWeightKg(75.0));

        mockMvc.perform(get("/api/v1/stats/volume")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31")
                        .with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.volumeKg").value(600.0));
    }

    @Test
    @DisplayName("Should return 400 when date-range volume is inverted")
    void getVolume_shouldReturn400WhenRangeIsInverted() throws Exception {
        mockMvc.perform(get("/api/v1/stats/volume")
                        .param("from", "2026-02-01")
                        .param("to", "2026-01-01")
                        .with(asUser(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return muscle intensity metadata and scores for the requested range")
    void getMuscleIntensity_shouldReturn200WithMuscleData() throws Exception {
        String muscleSlug = "controller-target-" + UUID.randomUUID();
        Muscle muscle = testEntityFactory.createAndPersistMuscle(MuscleBuilder.aMuscle(testEntityFactory.faker())
                .withName("Controller Target")
                .withSlug(muscleSlug)
                .withBodyRegion(BodyRegion.CHEST));
        var exercise = testEntityFactory.createAndPersistExerciseWithMuscles(
                ExerciseBuilder.anExercise(testEntityFactory.faker()).withCategory(Category.STRENGTH),
                List.of(muscle),
                ImpactLevel.PRIMARY);
        Instant completedAt = Instant.parse("2026-01-15T10:00:00Z");
        WorkoutSession session = testEntityFactory.createAndPersistWorkoutSession(
                WorkoutSessionBuilder.aWorkoutSession(testEntityFactory.faker())
                        .forUser(user)
                        .withStatus(SessionStatus.COMPLETED)
                        .withStartedAt(completedAt.minusSeconds(3600))
                        .withCompletedAt(completedAt));
        var sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withCompleted(true)
                .withReps(10)
                .withWeightKg(100.0));

        mockMvc.perform(get("/api/v1/stats/muscle-intensity")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31")
                        .with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-01-01"))
                .andExpect(jsonPath("$.to").value("2026-01-31"))
                .andExpect(jsonPath("$.muscles").isArray())
                .andExpect(jsonPath("$.muscles[?(@.slug == '%s')].id", muscleSlug)
                        .value(contains(muscle.getId().toString())))
                .andExpect(jsonPath("$.muscles[?(@.slug == '%s')].name", muscleSlug)
                        .value(contains("Controller Target")))
                .andExpect(jsonPath("$.muscles[?(@.slug == '%s')].bodyRegion", muscleSlug)
                        .value(contains("CHEST")))
                .andExpect(jsonPath("$.muscles[?(@.slug == '%s')].intensity", muscleSlug)
                        .value(contains(10.0)));
    }

    @Test
    @DisplayName("Should isolate muscle intensity from another user's completed sets")
    void getMuscleIntensity_shouldNotExposeAnotherUsersStimulus() throws Exception {
        Muscle requesterMuscle =
                testEntityFactory.createAndPersistMuscle(MuscleBuilder.aMuscle(testEntityFactory.faker())
                        .withName("Requester Catalog Muscle")
                        .withSlug("requester-catalog-muscle")
                        .withBodyRegion(BodyRegion.ARMS));
        String otherMuscleSlug = "other-user-muscle-" + UUID.randomUUID();
        Muscle otherMuscle = testEntityFactory.createAndPersistMuscle(MuscleBuilder.aMuscle(testEntityFactory.faker())
                .withName("Other User Muscle")
                .withSlug(otherMuscleSlug)
                .withBodyRegion(BodyRegion.LEGS));
        var otherExercise = testEntityFactory.createAndPersistExerciseWithMuscles(
                ExerciseBuilder.anExercise(testEntityFactory.faker()).withCategory(Category.STRENGTH),
                List.of(otherMuscle),
                ImpactLevel.PRIMARY);
        User otherUser = testEntityFactory.createAndPersistUser(
                UserBuilder.aUser(testEntityFactory.faker()).withTimezone("UTC"));
        Instant completedAt = Instant.parse("2026-01-15T10:00:00Z");
        WorkoutSession otherSession = testEntityFactory.createAndPersistWorkoutSession(
                WorkoutSessionBuilder.aWorkoutSession(testEntityFactory.faker())
                        .forUser(otherUser)
                        .withStatus(SessionStatus.COMPLETED)
                        .withStartedAt(completedAt.minusSeconds(3600))
                        .withCompletedAt(completedAt));
        var otherSessionExercise = testEntityFactory.createAndPersistSessionExercise(otherSession, otherExercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(otherSessionExercise)
                .withCompleted(true)
                .withReps(20)
                .withWeightKg(100.0));

        mockMvc.perform(get("/api/v1/stats/muscle-intensity")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31")
                        .with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.muscles[?(@.slug == '%s')].intensity", requesterMuscle.getSlug())
                        .value(contains(0.0)))
                .andExpect(jsonPath("$.muscles[?(@.slug == '%s')].intensity", otherMuscleSlug)
                        .value(contains(0.0)));
    }

    @Test
    @DisplayName("Should return 401 for unauthenticated muscle intensity requests")
    void getMuscleIntensity_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/stats/muscle-intensity")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 400 when the muscle intensity range is invalid")
    void getMuscleIntensity_shouldReturn400WhenRangeIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/stats/muscle-intensity")
                        .param("from", "2026-02-01")
                        .param("to", "2026-01-01")
                        .with(asUser(user)))
                .andExpect(status().isBadRequest());
    }
}
