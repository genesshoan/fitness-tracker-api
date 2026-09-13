package dev.genesshoan.fitnesstrackerapi.integration;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionSetBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.WorkoutSessionBuilder;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionStatus;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("Integration Tests - Stats Controller")
class StatsControllerIT extends AbstractIntegrationTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = testEntityFactory.createAndPersistUser();
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
        testEntityFactory.createAndPersistSessionSet(
                SessionSetBuilder.aSessionSet(testEntityFactory.faker())
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
    @DisplayName("Should return 404 when requesting another user's session volume")
    void getSessionVolume_shouldReturn404WhenSessionBelongsToAnotherUser() throws Exception {
        User otherUser = testEntityFactory.createAndPersistUser();
        WorkoutSession otherSession = testEntityFactory.createAndPersistWorkoutSession(otherUser);

        mockMvc.perform(get("/api/v1/stats/volume")
                        .param("sessionId", otherSession.getId().toString())
                        .with(asUser(user)))
                .andExpect(status().isNotFound());
    }
}
