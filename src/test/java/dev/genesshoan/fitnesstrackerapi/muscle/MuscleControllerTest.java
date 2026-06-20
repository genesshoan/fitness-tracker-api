package dev.genesshoan.fitnesstrackerapi.muscle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.genesshoan.fitnesstrackerapi.AbstractWebMvcTest;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.MuscleController;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.MuscleService;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.BodyRegion;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.dto.MuscleResponseDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MuscleController.class)
public class MuscleControllerTest extends AbstractWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MuscleService muscleService;

    @Test
    @WithMockUser
    void getMuscles_shouldReturn200AndPaginatedMuscles() throws Exception {
        Page<MuscleResponseDTO> page = new PageImpl<>(
            List.of(new MuscleResponseDTO("Biceps", BodyRegion.ARMS))
        );
        when(muscleService.getMuscles(any(Pageable.class))).thenReturn(page);

        mockMvc
            .perform(
                get("/api/v1/muscles")
                    .param("page", "0")
                    .param("size", "10")
                    .param("sort", "name, asc")
            )
            .andExpectAll(
                status().isOk(),
                jsonPath("$.content[0].name").value("Biceps"),
                jsonPath("$.content[0].body_region").value("ARMS")
            );
    }

    @Test
    @WithMockUser
    void getMuscle_shouldReturn200AndPaginatedMuscles() throws Exception {
        var dto = new MuscleResponseDTO("Biceps", BodyRegion.ARMS);
        when(muscleService.getMuscleBySlug(anyString())).thenReturn(dto);

        mockMvc
            .perform(get("/api/v1/muscles/biceps"))
            .andExpectAll(
                status().isOk(),
                jsonPath("$.name").value("Biceps"),
                jsonPath("$.body_region").value("ARMS")
            );
    }

    @Test
    @WithMockUser
    void getMuscle_shouldReturn404() throws Exception {
        when(muscleService.getMuscleBySlug(anyString())).thenThrow(
            new ResourceNotFoundException("Not Found")
        );

        mockMvc
            .perform(get("/api/v1/muscles/test"))
            .andExpect(status().isNotFound());
    }

    void shouldReturnUnauthorizedForProtectedEndpoints() throws Exception {
        mockMvc
            .perform(get("/api/v1/muscles"))
            .andExpect(status().isUnauthorized());

        mockMvc
            .perform(get("/api/v1/muscles/test"))
            .andExpect(status().isUnauthorized());
    }
}
