package dev.genesshoan.fitnesstrackerapi.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MuscleControllerIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("Should return 200 with muscles list")
    @WithMockUser
    void getMuscles_ShouldReturn200WithData() throws Exception {
        testEntityFactory.createAndPersistMuscle();

        mockMvc
            .perform(get("/api/v1/muscles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("Should return 200 with muscle by slug")
    @WithMockUser
    void getMuscleBySlug_ShouldReturn200() throws Exception {
        var muscle = testEntityFactory.createAndPersistMuscle();

        mockMvc
            .perform(get("/api/v1/muscles/{slug}", muscle.getSlug()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value(muscle.getSlug()))
            .andExpect(jsonPath("$.name").value(muscle.getName()));
    }

    @Test
    @DisplayName("Should return 404 when muscle not found")
    @WithMockUser
    void getMuscleBySlug_ShouldReturn404WhenNotFound() throws Exception {
        mockMvc
            .perform(get("/api/v1/muscles/non-existent-slug"))
            .andExpect(status().isNotFound());
    }
}
