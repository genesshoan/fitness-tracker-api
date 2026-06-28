package dev.genesshoan.fitnesstrackerapi.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ExerciseControllerIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("Should return 200 with exercises list")
    @WithMockUser
    void getAllExercises_ShouldReturn200WithData() throws Exception {
        testEntityFactory.createAndPersistExercise();

        mockMvc
            .perform(get("/api/v1/exercises"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").isArray())
            .andExpect(jsonPath("$.page.length()").value(1));
    }

    @Test
    @DisplayName("Should return 200 with exercise muscles populated")
    @WithMockUser
    void getExerciseBySlug_ShouldReturn200WithMusclesPopulated()
        throws Exception {
        var exercise = testEntityFactory.createAndPersistExerciseWithMuscles(
            2,
            ImpactLevel.PRIMARY
        );

        mockMvc
            .perform(get("/api/v1/exercises/{slug}", exercise.getSlug()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value(exercise.getSlug()))
            .andExpect(jsonPath("$.exercise_muscles").isArray())
            .andExpect(jsonPath("$.exercise_muscles.length()").value(2));
    }

    @Test
    @DisplayName("Should return 404 when exercise not found")
    @WithMockUser
    void getExerciseBySlug_ShouldReturn404WhenNotFound() throws Exception {
        mockMvc
            .perform(get("/api/v1/exercises/non-existent-slug"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when size exceeds max")
    @WithMockUser
    void getAllExercises_ShouldReturn400WhenSizeExceedsMax() throws Exception {
        mockMvc
            .perform(get("/api/v1/exercises").param("size", "101"))
            .andExpect(status().isBadRequest());
    }
}
