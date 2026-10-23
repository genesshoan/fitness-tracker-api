package dev.genesshoan.fitnesstrackerapi.exercise.controller;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class MuscleControllerIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("Should return 200 with muscles list")
    @WithMockUser
    void getMuscles_ShouldReturn200WithData() throws Exception {
        var muscle = testEntityFactory.createAndPersistMuscle();

        mockMvc.perform(get("/api/v1/muscles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].slug").value(muscle.getSlug()))
                .andExpect(jsonPath("$.content[0].bodyRegion")
                        .value(muscle.getBodyRegion().name()));
    }

    @Test
    @DisplayName("Should return 401 when listing muscles without authentication")
    void getMuscles_ShouldReturn401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/muscles")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 200 with muscle by slug")
    @WithMockUser
    void getMuscleBySlug_ShouldReturn200() throws Exception {
        var muscle = testEntityFactory.createAndPersistMuscle();

        mockMvc.perform(get("/api/v1/muscles/{slug}", muscle.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(muscle.getSlug()))
                .andExpect(jsonPath("$.name").value(muscle.getName()));
    }

    @Test
    @DisplayName("Should return 404 when muscle not found")
    @WithMockUser
    void getMuscleBySlug_ShouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/muscles/non-existent-slug")).andExpect(status().isNotFound());
    }
}
