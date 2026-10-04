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
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ExerciseBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    @DisplayName("Should return 401 when listing exercises without authentication")
    void getAllExercises_ShouldReturn401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/exercises")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should search exercises for autocomplete")
    @WithMockUser
    void searchExercises_ShouldReturnMatchingResultsWithHighlightedName() throws Exception {
        testEntityFactory.createAndPersistExercise(ExerciseBuilder.anExercise(testEntityFactory.faker())
                .withName("Barbell Bench Press")
                .withSlug("barbell-bench-press"));
        testEntityFactory.createAndPersistExercise(ExerciseBuilder.anExercise(testEntityFactory.faker())
                .withName("Dumbbell Row")
                .withSlug("dumbbell-row"));

        mockMvc.perform(get("/api/v1/exercises/search").param("q", "barbell").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(1))
                .andExpect(jsonPath("$.results[0].name").value("Barbell Bench Press"))
                .andExpect(jsonPath("$.results[0].slug").value("barbell-bench-press"))
                .andExpect(jsonPath("$.results[0].highlightedName").value("<b>Barbell</b> Bench Press"));
    }

    @Test
    @DisplayName("Should treat LIKE metacharacters literally when searching")
    @WithMockUser
    void searchExercises_ShouldTreatLikeMetacharactersLiterally() throws Exception {
        testEntityFactory.createAndPersistExercise(ExerciseBuilder.anExercise(testEntityFactory.faker())
                .withName("100% Effort")
                .withSlug("100-percent-effort"));
        testEntityFactory.createAndPersistExercise(ExerciseBuilder.anExercise(testEntityFactory.faker())
                .withName("Barbell Bench Press")
                .withSlug("barbell-bench-press"));

        mockMvc.perform(get("/api/v1/exercises/search").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(1))
                .andExpect(jsonPath("$.results[0].name").value("100% Effort"));
    }

    @Test
    @DisplayName("Should return 400 when autocomplete query is blank")
    @WithMockUser
    void searchExercises_ShouldReturn400ForBlankQuery() throws Exception {
        mockMvc.perform(get("/api/v1/exercises/search").param("q", " ")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject invalid autocomplete limits")
    @WithMockUser
    void searchExercises_ShouldReturn400ForInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/v1/exercises/search").param("q", "press").param("limit", "21"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 when searching exercises without authentication")
    void searchExercises_ShouldReturn401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/exercises/search").param("q", "press")).andExpect(status().isUnauthorized());
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
    @DisplayName("Should filter exercises by muscle slug")
    @WithMockUser
    void getAllExercises_ShouldFilterByMuscleSlug() throws Exception {
        var muscle = testEntityFactory.createAndPersistMuscle();
        testEntityFactory.createAndPersistExerciseWithMuscles(
                ExerciseBuilder.anExercise(testEntityFactory.faker()), List.of(muscle), ImpactLevel.PRIMARY);
        testEntityFactory.createAndPersistExercise();

        mockMvc.perform(get("/api/v1/exercises").param("muscleSlugs", muscle.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.length()").value(1))
                .andExpect(jsonPath("$.page[0].slug").isNotEmpty());
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
    @DisplayName("Should update exercise and replace its muscles")
    @WithMockUser(roles = "ADMIN")
    void updateExercise_ShouldReplaceMuscleAssociations() throws Exception {
        var exercise = testEntityFactory.createAndPersistExerciseWithMuscles(1, ImpactLevel.PRIMARY);
        var muscle = testEntityFactory.createAndPersistMuscle();
        var request = new ExerciseRequestDTO(
                "Updated Exercise",
                "updated-exercise",
                "Updated description",
                List.of("Updated step"),
                Category.MOBILITY,
                Difficulty.ADVANCED,
                List.of(new ExerciseMuscleRequestDTO(muscle.getSlug(), ImpactLevel.SECONDARY)));

        mockMvc.perform(put("/api/v1/exercises/{slug}", exercise.getSlug())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("updated-exercise"))
                .andExpect(jsonPath("$.category").value("MOBILITY"))
                .andExpect(jsonPath("$.exerciseMuscles.length()").value(1))
                .andExpect(jsonPath("$.exerciseMuscles[0].muscle.slug").value(muscle.getSlug()))
                .andExpect(jsonPath("$.exerciseMuscles[0].impactLevel").value("SECONDARY"));
    }

    @Test
    @DisplayName("Should return 401 when reading exercise without authentication")
    void getExerciseBySlug_ShouldReturn401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/exercises/example-slug")).andExpect(status().isUnauthorized());
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
