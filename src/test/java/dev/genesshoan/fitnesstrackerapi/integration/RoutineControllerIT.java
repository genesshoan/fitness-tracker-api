package dev.genesshoan.fitnesstrackerapi.integration;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineResponseDTO;
import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Integration Tests - Routine Controller")
class RoutineControllerIT extends AbstractIntegrationTest {

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
    @DisplayName("GET /api/v1/routines")
    class GetRoutines {

        @Test
        @DisplayName("Should return 200 with paginated routines")
        void shouldReturn200WithPaginatedRoutines() throws Exception {
            testEntityFactory.createAndPersistRoutine(user, "Routine 1");
            testEntityFactory.createAndPersistRoutine(user, "Routine 2");
            testEntityFactory.createAndPersistRoutine(user, "Routine 3");

            mockMvc.perform(get("/api/v1/routines")
                            .param("page", "0")
                            .param("size", "10")
                            .with(asUser(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.totalElements").value(3));
        }

        @Test
        @DisplayName("Should return 401 when no auth token")
        void shouldReturn401WhenNoAuth() throws Exception {
            mockMvc.perform(get("/api/v1/routines")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/routines/{routineId}")
    class GetRoutineById {

        private Routine routine;

        @BeforeEach
        void setUp() {
            Exercise ex1 = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);
            Exercise ex2 = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);

            routine = testEntityFactory.createAndPersistRoutineWithExercises(user, List.of(ex1, ex2));
        }

        @Test
        @DisplayName("Should return 200 with routine when user owns it")
        void shouldReturn200WhenUserOwnsRoutine() throws Exception {
            mockMvc.perform(get("/api/v1/routines/{routineId}", routine.getId()).with(asUser(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(routine.getId().toString()))
                    .andExpect(jsonPath("$.name").value(routine.getName()))
                    .andExpect(jsonPath("$.exercises", hasSize(2)))
                    .andExpect(jsonPath("$.exercises[0].position").value(1))
                    .andExpect(jsonPath("$.exercises[1].position").value(2));
        }

        @Test
        @DisplayName("Should return 404 when routine does not exist")
        void shouldReturn404WhenRoutineNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/routines/{routineId}", UUID.randomUUID())
                            .with(asUser(user)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 (not 403) when the routine belongs to another user")
        void shouldReturn404WhenUserDoesNotOwnRoutine() throws Exception {
            User otherUser = testEntityFactory.createAndPersistUser();
            Routine otherRoutine = testEntityFactory.createAndPersistRoutine(otherUser, "Other Routine");

            mockMvc.perform(get("/api/v1/routines/{routineId}", otherRoutine.getId())
                            .with(asUser(user)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/routines")
    class CreateRoutine {

        @Test
        @DisplayName("Should return 201 with created routine")
        void shouldReturn201WhenValid() throws Exception {
            Exercise exercise = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);

            var request = Map.of(
                    "name", "Push Day",
                    "description", "Push exercises",
                    "exercises",
                            List.of(Map.of(
                                    "exerciseId",
                                    exercise.getId().toString(),
                                    "defaultRestSeconds",
                                    60,
                                    "defaultSets",
                                    3,
                                    "defaultReps",
                                    12,
                                    "defaultWeightKg",
                                    14.0,
                                    "notes",
                                    "Test note")));

            mockMvc.perform(post("/api/v1/routines")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(asUser(user)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Push Day"))
                    .andExpect(jsonPath("$.exercises", hasSize(1)))
                    .andExpect(jsonPath("$.exercises[0].exercise.id")
                            .value(exercise.getId().toString()));
        }

        @Test
        @DisplayName("Should return 400 when request body is invalid (missing name)")
        void shouldReturn400WhenNameMissing() throws Exception {
            var request = Map.of("description", "No name", "exercises", List.of());

            mockMvc.perform(post("/api/v1/routines")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(asUser(user)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 when routine name already exists")
        @Transactional
        void shouldReturn409WhenNameExists() throws Exception {
            testEntityFactory.createAndPersistRoutine(user, "Existing Routine");

            var request = Map.of("name", "Existing Routine", "description", "Duplicate", "exercises", List.of());

            mockMvc.perform(post("/api/v1/routines")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(asUser(user)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/routines/{routineId}")
    class UpdateRoutine {

        private Routine routine;

        @BeforeEach
        void setUp() {
            Exercise ex1 = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);
            Exercise ex2 = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);

            routine = testEntityFactory.createAndPersistRoutineWithExercises(user, List.of(ex1, ex2));
        }

        @Test
        @DisplayName("Should return 200 with updated routine")
        @Transactional
        void shouldReturn200WhenValid() throws Exception {
            Exercise newExercise = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);

            var request = Map.of(
                    "name", "Updated Push Day",
                    "description", "Updated description",
                    "exercises",
                            List.of(Map.of(
                                    "exerciseId",
                                    newExercise.getId().toString(),
                                    "defaultRestSeconds",
                                    90,
                                    "defaultSets",
                                    4,
                                    "defaultReps",
                                    10,
                                    "defaultWeightKg",
                                    20.0,
                                    "notes",
                                    "Updated note")));

            mockMvc.perform(put("/api/v1/routines/{routineId}", routine.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(asUser(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Push Day"))
                    .andExpect(jsonPath("$.exercises", hasSize(1)))
                    .andExpect(jsonPath("$.exercises[0].exercise.id")
                            .value(newExercise.getId().toString()))
                    .andExpect(jsonPath("$.exercises[0].defaultSets").value(4));
        }

        @Test
        @DisplayName("Should return 404 when routine does not exist")
        void shouldReturn404WhenRoutineNotFound() throws Exception {
            var request = Map.of("name", "New Name", "description", "New Desc", "exercises", List.of());

            mockMvc.perform(put("/api/v1/routines/{routineId}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(asUser(user)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 409 when new name conflicts with another routine")
        @Transactional
        void shouldReturn409WhenNameConflicts() throws Exception {
            testEntityFactory.createAndPersistRoutine(user, "Conflicting Name");

            var request = Map.of("name", "Conflicting Name", "description", "Should conflict", "exercises", List.of());

            mockMvc.perform(put("/api/v1/routines/{routineId}", routine.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(asUser(user)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/routines/{routineId}")
    class DeleteRoutine {

        private Routine routine;

        @BeforeEach
        void setUp() {
            routine = testEntityFactory.createAndPersistRoutine(user, "To Delete");
        }

        @Test
        @DisplayName("Should return 204 and soft delete the routine")
        @Transactional
        void shouldReturn204AndSoftDelete() throws Exception {
            mockMvc.perform(delete("/api/v1/routines/{routineId}", routine.getId())
                            .with(asUser(user)))
                    .andExpect(status().isNoContent());

            var deletedRoutine = testEntityFactory.getRoutineRepository().findById(routine.getId());
            assertThat(deletedRoutine).isPresent();
            assertThat(deletedRoutine.get().isActive()).isFalse();
        }

        @Test
        @DisplayName("Should return 404 when routine does not exist")
        void shouldReturn404WhenRoutineNotFound() throws Exception {
            mockMvc.perform(delete("/api/v1/routines/{routineId}", UUID.randomUUID())
                            .with(asUser(user)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/routines/{routineId}/exercises/{position}")
    class AddRoutineExercise {

        private Routine routine;
        private Exercise existingExercise;

        @BeforeEach
        void setUp() {
            existingExercise = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);
            routine = testEntityFactory.createAndPersistRoutineWithExercise(user, existingExercise);
        }

        @Test
        @DisplayName("Should return 200 and add exercise at position")
        @Transactional
        void shouldReturn200AndAddExercise() throws Exception {
            Exercise newExercise = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);

            var request = Map.of(
                    "exerciseId",
                    newExercise.getId().toString(),
                    "defaultRestSeconds",
                    60,
                    "defaultSets",
                    3,
                    "defaultReps",
                    12,
                    "defaultWeightKg",
                    14.0,
                    "notes",
                    "New exercise");

            mockMvc.perform(post("/api/v1/routines/{routineId}/exercises/{position}", routine.getId(), 1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(asUser(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exercises", hasSize(2)))
                    .andExpect(jsonPath("$.exercises[0].exercise.id")
                            .value(newExercise.getId().toString()))
                    .andExpect(jsonPath("$.exercises[0].position").value(1))
                    .andExpect(jsonPath("$.exercises[1].exercise.id")
                            .value(existingExercise.getId().toString()))
                    .andExpect(jsonPath("$.exercises[1].position").value(2));
        }

        @Test
        @DisplayName("Should return 400 when position < 1")
        void shouldReturn400WhenPositionInvalid() throws Exception {
            var request = Map.of(
                    "exerciseId", UUID.randomUUID().toString(),
                    "defaultRestSeconds", 60,
                    "defaultSets", 3,
                    "defaultReps", 12,
                    "defaultWeightKg", 14.0);

            mockMvc.perform(post("/api/v1/routines/{routineId}/exercises/{position}", routine.getId(), 0)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(asUser(user)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when exercise does not exist")
        void shouldReturn404WhenExerciseNotFound() throws Exception {
            var request = Map.of(
                    "exerciseId", UUID.randomUUID().toString(),
                    "defaultRestSeconds", 60,
                    "defaultSets", 3,
                    "defaultReps", 12,
                    "defaultWeightKg", 14.0);

            mockMvc.perform(post("/api/v1/routines/{routineId}/exercises/{position}", routine.getId(), 1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(asUser(user)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/routines/{routineId}/exercises/{position}")
    class UpdateRoutineExercise {

        private Routine routine;
        private Exercise ex2;

        @BeforeEach
        void setUp() {
            Exercise ex1 = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);
            ex2 = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);

            routine = testEntityFactory.createAndPersistRoutineWithExercises(user, List.of(ex1, ex2));
        }

        @Test
        @DisplayName("Should return 200 and update exercise")
        @Transactional
        void shouldReturn200AndUpdateExercise() throws Exception {
            Exercise newExercise = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);

            var request = Map.of(
                    "exerciseId",
                    newExercise.getId().toString(),
                    "defaultRestSeconds",
                    120,
                    "defaultSets",
                    5,
                    "defaultReps",
                    8,
                    "defaultWeightKg",
                    25.0,
                    "notes",
                    "Updated exercise");

            mockMvc.perform(put("/api/v1/routines/{routineId}/exercises/{position}", routine.getId(), 1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(asUser(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exercises", hasSize(2)))
                    .andExpect(jsonPath("$.exercises[0].exercise.id")
                            .value(newExercise.getId().toString()))
                    .andExpect(jsonPath("$.exercises[0].defaultSets").value(5))
                    .andExpect(jsonPath("$.exercises[0].defaultWeightKg").value(25.0))
                    .andExpect(jsonPath("$.exercises[1].exercise.id")
                            .value(ex2.getId().toString()));
        }

        @Test
        @DisplayName("Should return 404 when there is no exercise at the given position")
        void shouldReturn404WhenPositionDoesNotExist() throws Exception {
            var request = Map.of(
                    "exerciseId", UUID.randomUUID().toString(),
                    "defaultRestSeconds", 60,
                    "defaultSets", 3,
                    "defaultReps", 12,
                    "defaultWeightKg", 14.0);

            mockMvc.perform(put("/api/v1/routines/{routineId}/exercises/{position}", routine.getId(), 99)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(asUser(user)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/routines/{routineId}/exercises/{position}")
    class DeleteRoutineExercise {

        private Routine routine;
        private Exercise ex1;
        private Exercise ex3;

        @BeforeEach
        void setUp() {
            ex1 = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);
            Exercise ex2 = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);
            ex3 = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);

            routine = testEntityFactory.createAndPersistRoutineWithExercises(user, List.of(ex1, ex2, ex3));
        }

        @Test
        @DisplayName("Should return 204 and remove exercise, shifting positions")
        @Transactional
        void shouldReturn204AndRemoveExercise() throws Exception {
            mockMvc.perform(delete("/api/v1/routines/{routineId}/exercises/{position}", routine.getId(), 2)
                            .with(asUser(user)))
                    .andExpect(status().isNoContent());

            var result = mockMvc.perform(
                            get("/api/v1/routines/{routineId}", routine.getId()).with(asUser(user)))
                    .andExpect(status().isOk())
                    .andReturn();

            var routineResponse =
                    objectMapper.readValue(result.getResponse().getContentAsString(), RoutineResponseDTO.class);

            assertThat(routineResponse.exercises()).hasSize(2);
            assertThat(routineResponse.exercises())
                    .extracting(e -> e.exercise().id())
                    .containsExactly(ex1.getId(), ex3.getId());
            assertThat(routineResponse.exercises())
                    .extracting(e -> e.position())
                    .containsExactly(1, 2);
        }

        @Test
        @DisplayName("Should return 404 when there is no exercise at the given position")
        void shouldReturn404WhenPositionDoesNotExist() throws Exception {
            mockMvc.perform(delete("/api/v1/routines/{routineId}/exercises/{position}", routine.getId(), 99)
                            .with(asUser(user)))
                    .andExpect(status().isNotFound());
        }
    }
}
