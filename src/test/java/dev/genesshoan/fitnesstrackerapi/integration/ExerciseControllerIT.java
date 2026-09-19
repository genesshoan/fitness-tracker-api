package dev.genesshoan.fitnesstrackerapi.integration;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseMuscleRequestDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class ExerciseControllerIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("Should return 200 with exercises list")
    @WithMockUser
    void getAllExercises_ShouldReturn200WithData() throws Exception {
        testEntityFactory.createAndPersistExercise();

        mockMvc.perform(get("/api/v1/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").isArray())
                .andExpect(jsonPath("$.page.length()").value(1));
    }

    @Test
    @DisplayName("Should return 200 with exercise muscles populated")
    @WithMockUser
    void getExerciseBySlug_ShouldReturn200WithMusclesPopulated() throws Exception {
        var exercise = testEntityFactory.createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY);

        mockMvc.perform(get("/api/v1/exercises/{slug}", exercise.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(exercise.getSlug()))
                .andExpect(jsonPath("$.instructions").isArray())
                .andExpect(jsonPath("$.instructions.length()").value(2))
                .andExpect(jsonPath("$.gifUrl").value(nullValue()))
                .andExpect(jsonPath("$.exerciseMuscles").isArray())
                .andExpect(jsonPath("$.exerciseMuscles.length()").value(2));
    }

    @Test
    @DisplayName("Should return 404 when exercise not found")
    @WithMockUser
    void getExerciseBySlug_ShouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/exercises/non-existent-slug")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when size exceeds max")
    @WithMockUser
    void getAllExercises_ShouldReturn400WhenSizeExceedsMax() throws Exception {
        mockMvc.perform(get("/api/v1/exercises").param("size", "101")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 403 when deleting without ADMIN role")
    @WithMockUser
    void deleteExercise_ShouldReturn403WithoutAdminRole() throws Exception {
        var exercise = testEntityFactory.createAndPersistExercise();

        mockMvc.perform(delete("/api/v1/exercises/{slug}", exercise.getSlug())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 when creating without ADMIN role")
    @WithMockUser
    void createExercise_ShouldReturn403WithoutAdminRole() throws Exception {
        var request = new ExerciseRequestDTO(
                "Bicep Curl",
                "bicep-curl",
                "Made with a bicep curl bar",
                List.of("Step 1"),
                Category.STRENGTH,
                Difficulty.BEGINNER,
                null);

        mockMvc.perform(post("/api/v1/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should create exercise with muscles and query it back")
    @WithMockUser(roles = "ADMIN")
    void createExercise_ShouldCreateWithMusclesAndBeQueryable() throws Exception {
        var muscle = testEntityFactory.createAndPersistMuscle();

        var request = new ExerciseRequestDTO(
                "Bicep Curl",
                "bicep-curl",
                "Made with a bicep curl bar",
                List.of("Step 1", "Step 2"),
                Category.STRENGTH,
                Difficulty.BEGINNER,
                List.of(new ExerciseMuscleRequestDTO(muscle.getSlug(), ImpactLevel.PRIMARY)));

        mockMvc.perform(post("/api/v1/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("bicep-curl"))
                .andExpect(jsonPath("$.instructions.length()").value(2))
                .andExpect(jsonPath("$.exerciseMuscles.length()").value(1))
                .andExpect(jsonPath("$.exerciseMuscles[0].impactLevel").value("PRIMARY"));

        mockMvc.perform(get("/api/v1/exercises/{slug}", "bicep-curl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bicep Curl"))
                .andExpect(jsonPath("$.exerciseMuscles.length()").value(1));
    }

    @Test
    @DisplayName("Should soft-delete exercise and hide it from reads")
    @WithMockUser(roles = "ADMIN")
    void deleteExercise_ShouldSoftDeleteAndHideFromReads() throws Exception {
        var exercise = testEntityFactory.createAndPersistExercise();

        mockMvc.perform(delete("/api/v1/exercises/{slug}", exercise.getSlug())).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/exercises/{slug}", exercise.getSlug())).andExpect(status().isNotFound());
    }
}
