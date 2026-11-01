package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import dev.genesshoan.fitnesstrackerapi.gamification.application.service.AchievementEvaluationService;
import dev.genesshoan.fitnesstrackerapi.security.UserDetailsImpl;
import dev.genesshoan.fitnesstrackerapi.testdata.TestEntityFactory;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionSetBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.UserBuilder;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("Integration Tests - Achievement Controller")
class AchievementControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AchievementEvaluationService achievementEvaluationService;

    @Autowired
    private TestEntityFactory testEntityFactory;

    private User user;

    @BeforeEach
    void setUp() {
        user = testEntityFactory.createAndPersistUser(
                UserBuilder.aUser(testEntityFactory.faker()).withTimezone("UTC"));
    }

    @Test
    @DisplayName("Should return 200 with the catalog and the state of the user")
    void getCatalog_shouldReturnTheCatalogWithTheState() throws Exception {
        // Given
        trainAndEvaluate();

        // When
        mockMvc.perform(get("/api/v1/achievements").with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].code").value("FIRST_WORKOUT"))
                .andExpect(jsonPath("$[0].unlocked").value(true))
                .andExpect(jsonPath("$[0].progressRatio").value(1.0));
    }

    @Test
    @DisplayName("Should return 200 with the entries of one achievement")
    void getByCode_shouldReturnTheEntriesOfTheAchievement() throws Exception {
        // Given
        trainAndEvaluate();

        // When
        mockMvc.perform(get("/api/v1/achievements/FIRST_WORKOUT").with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("FIRST_WORKOUT"))
                .andExpect(jsonPath("$[0].unlocked").value(true))
                .andExpect(jsonPath("$[0].unlockedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 404 for an unknown achievement")
    void getByCode_shouldReturn404ForAnUnknownAchievement() throws Exception {
        // When
        mockMvc.perform(get("/api/v1/achievements/NOT_A_REAL_ACHIEVEMENT").with(asUser(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 200 with the feed of unlocked achievements")
    void getFeed_shouldReturnTheUnlockedAchievements() throws Exception {
        // Given
        trainAndEvaluate();

        // When
        mockMvc.perform(get("/api/v1/achievements/feed").with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].code").value("FIRST_WORKOUT"))
                .andExpect(jsonPath("$[0].seen").value(false))
                .andExpect(jsonPath("$[?(@.code == 'PR_FIRST_MAX_WEIGHT')]").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 200 with the state of the collection")
    void getSummary_shouldReturnTheStateOfTheCollection() throws Exception {
        // Given
        trainAndEvaluate();

        // When
        mockMvc.perform(get("/api/v1/achievements/summary").with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAvailable", greaterThan(0)))
                .andExpect(jsonPath("$.totalUnlocked", greaterThan(0)))
                .andExpect(jsonPath("$.unseenCount", greaterThan(0)))
                .andExpect(jsonPath("$.next").isNotEmpty())
                .andExpect(jsonPath("$.next[0].code").isNotEmpty())
                .andExpect(jsonPath("$.next[0].remaining").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 204 and empty the unseen feed when the achievements are marked as seen")
    void markSeen_shouldMarkTheAchievementsAsSeen() throws Exception {
        // Given
        trainAndEvaluate();

        String response = mockMvc.perform(get("/api/v1/achievements/feed").with(asUser(user)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> ids = new ObjectMapper().readTree(response).findValuesAsText("id");

        // When
        mockMvc.perform(post("/api/v1/achievements/seen")
                        .with(asUser(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementIds\":[\"" + ids.get(0) + "\"]}"))
                .andExpect(status().isNoContent());

        // Then
        mockMvc.perform(get("/api/v1/achievements/feed")
                        .param("unseenOnly", "true")
                        .with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(ids.size() - 1)))
                .andExpect(jsonPath("$[?(@.id == '" + ids.get(0) + "')]").isEmpty());
    }

    /**
     * Trains once and evaluates the achievements, so the user has state to read.
     */
    private void trainAndEvaluate() {
        var exercise = testEntityFactory.createAndPersistExercise();
        WorkoutSession session = testEntityFactory.createAndPersistCompletedWorkoutSession(user);
        var sessionExercise = testEntityFactory.createAndPersistSessionExercise(session, exercise);
        testEntityFactory.createAndPersistSessionSet(SessionSetBuilder.aSessionSet(testEntityFactory.faker())
                .forSessionExercise(sessionExercise)
                .withSetNumber(1)
                .withReps(10)
                .withWeightKg(100.0)
                .withCompleted(true));

        achievementEvaluationService.onSessionCompleted(user.getId(), session.getId());
    }

    private RequestPostProcessor asUser(User targetUser) {
        UserDetails userDetails = new UserDetailsImpl(
                targetUser.getId(),
                targetUser.getEmail(),
                targetUser.getEmail(),
                targetUser.getUsername(),
                targetUser.getPasswordHash(),
                targetUser.isEmailVerified(),
                List.of(new SimpleGrantedAuthority("ROLE_" + targetUser.getRole())));
        return SecurityMockMvcRequestPostProcessors.user(userDetails);
    }
}
