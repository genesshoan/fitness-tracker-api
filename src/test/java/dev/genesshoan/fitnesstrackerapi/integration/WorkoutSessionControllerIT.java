package dev.genesshoan.fitnesstrackerapi.integration;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionStatus;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Integration Tests - WorkoutSession Controller")
class WorkoutSessionControllerIT extends AbstractIntegrationTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = testEntityFactory.createAndPersistUser();
    }

    private RequestPostProcessor asUser(User targetUser) {
        UserDetails userDetails = new UserDetailsImpl(targetUser);
        return SecurityMockMvcRequestPostProcessors.user(userDetails);
    }

    @Nested
    @DisplayName("GET /api/v1/sessions")
    class GetSessions {

        @Test
        @DisplayName("Should return 200 with paginated sessions for authenticated user")
        void shouldReturn200WithPaginatedSessions() throws Exception {
            testEntityFactory.createAndPersistWorkoutSession(user);
            testEntityFactory.createAndPersistWorkoutSession(user);
            testEntityFactory.createAndPersistWorkoutSession(user);

            mockMvc.perform(get("/api/v1/sessions")
                            .param("page", "0")
                            .param("size", "10")
                            .with(asUser(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.totalElements").value(3));
        }

        @Test
        @DisplayName("Should return only the authenticated user's sessions")
        void shouldReturnOnlyAuthenticatedUserSessions() throws Exception {
            User otherUser = testEntityFactory.createAndPersistUser();
            testEntityFactory.createAndPersistWorkoutSession(user);
            testEntityFactory.createAndPersistWorkoutSession(otherUser);
            testEntityFactory.createAndPersistWorkoutSession(otherUser);

            mockMvc.perform(get("/api/v1/sessions")
                            .param("page", "0")
                            .param("size", "10")
                            .with(asUser(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should return 401 when no auth")
        void shouldReturn401WhenNoAuth() throws Exception {
            mockMvc.perform(get("/api/v1/sessions")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/sessions/{sessionId}")
    class GetSessionById {

        private WorkoutSession session;

        @BeforeEach
        void setUp() {
            session = testEntityFactory.createAndPersistWorkoutSessionWithExercises(user, 2);
        }

        @Test
        @DisplayName("Should return 200 with session details and ordered exercises when user owns it")
        void shouldReturn200WhenUserOwnsSession() throws Exception {
            mockMvc.perform(get("/api/v1/sessions/{sessionId}", session.getId()).with(asUser(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(session.getId().toString()))
                    .andExpect(jsonPath("$.status").value(SessionStatus.IN_PROGRESS.name()))
                    .andExpect(jsonPath("$.exercises").isArray())
                    .andExpect(jsonPath("$.exercises", hasSize(2)))
                    .andExpect(jsonPath("$.exercises[0].position").value(1))
                    .andExpect(jsonPath("$.exercises[1].position").value(2));
        }

        @Test
        @DisplayName("Should return 404 when session does not exist")
        void shouldReturn404WhenSessionNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/sessions/{sessionId}", UUID.randomUUID())
                            .with(asUser(user)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 (not 403) when session belongs to another user")
        void shouldReturn404WhenUserDoesNotOwnSession() throws Exception {
            User otherUser = testEntityFactory.createAndPersistUser();
            WorkoutSession otherSession = testEntityFactory.createAndPersistWorkoutSession(otherUser);

            mockMvc.perform(get("/api/v1/sessions/{sessionId}", otherSession.getId())
                            .with(asUser(user)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/sessions")
    class CreateWorkoutSessionFromScratch {

        @Test
        @DisplayName("Should return 201 and persist a new in-progress session with exercises")
        void shouldReturn201AndPersistSession() throws Exception {
            String body = "{\"status\":\"IN_PROGRESS\",\"startedAt\":\"2025-01-01T10:00:00Z\","
                    + "\"notes\":\"My session notes\",\"exercises\":[]}";

            mockMvc.perform(post("/api/v1/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .with(asUser(user)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.status").value(SessionStatus.IN_PROGRESS.name()))
                    .andExpect(jsonPath("$.notes").value("My session notes"))
                    .andExpect(jsonPath("$.exercises").isArray());
        }

        @Test
        @DisplayName("Should return 200 when retrieving a session without exercises")
        void shouldReturn200ForSessionWithoutExercises() throws Exception {
            String body = "{\"status\":\"IN_PROGRESS\",\"startedAt\":\"2025-01-01T10:00:00Z\","
                    + "\"notes\":\"Empty session\",\"exercises\":[]}";

            String sessionId = objectMapper
                    .readTree(mockMvc.perform(post("/api/v1/sessions")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body)
                                    .with(asUser(user)))
                            .andExpect(status().isCreated())
                            .andReturn()
                            .getResponse()
                            .getContentAsString())
                    .get("id")
                    .asText();

            mockMvc.perform(get("/api/v1/sessions/{sessionId}", sessionId).with(asUser(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notes").value("Empty session"))
                    .andExpect(jsonPath("$.exercises", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 400 when status is missing")
        void shouldReturn400WhenStatusMissing() throws Exception {
            String body = "{\"startedAt\":\"2024-01-01T00:00:00Z\",\"exercises\":[]}";

            mockMvc.perform(post("/api/v1/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .with(asUser(user)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when startedAt is missing")
        void shouldReturn400WhenStartedAtMissing() throws Exception {
            String body = "{\"status\":\"IN_PROGRESS\",\"exercises\":[]}";

            mockMvc.perform(post("/api/v1/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .with(asUser(user)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/sessions/from-routine/{routineId}")
    class CreateWorkoutSessionFromRoutine {

        @Test
        @DisplayName("Should return 201 and create session with copies of routine exercises")
        void shouldReturn201AndCreateSessionFromRoutine() throws Exception {
            var exercise1 = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);
            var exercise2 = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);
            Routine routine =
                    testEntityFactory.createAndPersistRoutineWithExercises(user, List.of(exercise1, exercise2));

            mockMvc.perform(post("/api/v1/sessions/from-routine/{routineId}", routine.getId())
                            .with(asUser(user)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.status").value(SessionStatus.IN_PROGRESS.name()))
                    .andExpect(jsonPath("$.exercises", hasSize(2)))
                    .andExpect(jsonPath("$.exercises[0].position").value(1))
                    .andExpect(jsonPath("$.exercises[1].position").value(2))
                    .andExpect(jsonPath("$.exercises[0].exercise.id")
                            .value(exercise1.getId().toString()))
                    .andExpect(jsonPath("$.exercises[1].exercise.id")
                            .value(exercise2.getId().toString()));
        }

        @Test
        @DisplayName("Should return 404 when routine does not exist")
        void shouldReturn404WhenRoutineNotFound() throws Exception {
            mockMvc.perform(post("/api/v1/sessions/from-routine/{routineId}", UUID.randomUUID())
                            .with(asUser(user)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when routine belongs to another user")
        void shouldReturn404WhenRoutineBelongsToAnotherUser() throws Exception {
            User otherUser = testEntityFactory.createAndPersistUser();
            Routine otherRoutine = testEntityFactory.createAndPersistRoutine(otherUser);

            mockMvc.perform(post("/api/v1/sessions/from-routine/{routineId}", otherRoutine.getId())
                            .with(asUser(user)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/sessions/{sessionId}/notes")
    class UpdateSessionNotes {

        private WorkoutSession session;

        @BeforeEach
        void setUp() {
            session = testEntityFactory.createAndPersistWorkoutSession(user);
        }

        @Test
        @DisplayName("Should return 204 and update the notes when session is in progress")
        void shouldReturn204AndUpdateNotes() throws Exception {
            mockMvc.perform(patch("/api/v1/sessions/{sessionId}/notes", session.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"notes\":\"New notes\"}")
                            .with(asUser(user)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/sessions/{sessionId}", session.getId()).with(asUser(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notes").value("New notes"));
        }

        @Test
        @DisplayName("Should return 400 when updating notes on a completed session")
        void shouldReturn400WhenSessionIsCompleted() throws Exception {
            mockMvc.perform(patch("/api/v1/sessions/{sessionId}/finish", session.getId())
                            .with(asUser(user)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(patch("/api/v1/sessions/{sessionId}/notes", session.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"notes\":\"New notes\"}")
                            .with(asUser(user)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when session does not exist")
        void shouldReturn404WhenSessionNotFound() throws Exception {
            mockMvc.perform(patch("/api/v1/sessions/{sessionId}/notes", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"notes\":\"New notes\"}")
                            .with(asUser(user)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/sessions/{sessionId}/finish")
    class CompleteSession {

        private WorkoutSession session;

        @BeforeEach
        void setUp() {
            session = testEntityFactory.createAndPersistWorkoutSession(user);
        }

        @Test
        @DisplayName("Should return 204 and set status to COMPLETED with completedAt")
        @Transactional
        void shouldReturn204AndCompleteSession() throws Exception {
            mockMvc.perform(patch("/api/v1/sessions/{sessionId}/finish", session.getId())
                            .with(asUser(user)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 when session does not exist")
        void shouldReturn404WhenSessionNotFound() throws Exception {
            mockMvc.perform(patch("/api/v1/sessions/{sessionId}/finish", UUID.randomUUID())
                            .with(asUser(user)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/sessions/{sessionId}")
    class DeleteSession {

        private WorkoutSession session;

        @BeforeEach
        void setUp() {
            session = testEntityFactory.createAndPersistWorkoutSession(user);
        }

        @Test
        @DisplayName("Should return 204 and remove the session")
        void shouldReturn204AndDeleteSession() throws Exception {
            mockMvc.perform(delete("/api/v1/sessions/{sessionId}", session.getId())
                            .with(asUser(user)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/sessions/{sessionId}", session.getId()).with(asUser(user)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when session does not exist")
        void shouldReturn404WhenSessionNotFound() throws Exception {
            mockMvc.perform(delete("/api/v1/sessions/{sessionId}", UUID.randomUUID())
                            .with(asUser(user)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("Should complete the full routine-to-finished-session workflow")
    void shouldCompleteRoutineToFinishedSessionWorkflow() throws Exception {
        var exercise1 = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);
        var exercise2 = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);
        Routine routine = testEntityFactory.createAndPersistRoutineWithExercises(user, List.of(exercise1, exercise2));

        JsonNode createdSession =
                objectMapper.readTree(mockMvc.perform(post("/api/v1/sessions/from-routine/{routineId}", routine.getId())
                                .with(asUser(user)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value(SessionStatus.IN_PROGRESS.name()))
                        .andExpect(jsonPath("$.exercises", hasSize(2)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        String sessionId = createdSession.get("id").asText();
        String firstExerciseId =
                createdSession.get("exercises").get(0).get("id").asText();
        String firstSetId = createdSession
                .get("exercises")
                .get(0)
                .get("sets")
                .get(0)
                .get("id")
                .asText();
        String secondSetId = createdSession
                .get("exercises")
                .get(0)
                .get("sets")
                .get(1)
                .get("id")
                .asText();
        String completedSetBody = "{\"reps\":12,\"weightKg\":30.0,\"completed\":true}";

        mockMvc.perform(put(
                                "/api/v1/sessions/{sessionId}/exercises/{exerciseId}/sets/{setId}",
                                sessionId,
                                firstExerciseId,
                                firstSetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completedSetBody)
                        .with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.reps").value(12));

        mockMvc.perform(put(
                                "/api/v1/sessions/{sessionId}/exercises/{exerciseId}/sets/{setId}",
                                sessionId,
                                firstExerciseId,
                                secondSetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completedSetBody)
                        .with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        mockMvc.perform(patch("/api/v1/sessions/{sessionId}/finish", sessionId).with(asUser(user)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/sessions/{sessionId}", sessionId).with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(SessionStatus.COMPLETED.name()))
                .andExpect(jsonPath("$.completedAt").exists())
                .andExpect(jsonPath("$.exercises[0].sets[0].completed").value(true))
                .andExpect(jsonPath("$.exercises[0].sets[1].completed").value(true));

        mockMvc.perform(put(
                                "/api/v1/sessions/{sessionId}/exercises/{exerciseId}/sets/{setId}",
                                sessionId,
                                firstExerciseId,
                                firstSetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completedSetBody)
                        .with(asUser(user)))
                .andExpect(status().isBadRequest());
    }
}
