package dev.genesshoan.fitnesstrackerapi.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPageRequest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseDetailDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseListItemDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.mapper.ExerciseMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private ExerciseMapper exerciseMapper;

    @InjectMocks
    private ExerciseService exerciseService;

    @Test
    void getAllExercise_shouldReturnPageWithNextCursorWhenMoreElementsExist() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        Exercise e1 = Exercise.builder().id(id1).build();
        Exercise e2 = Exercise.builder().id(id2).build();
        Exercise e3 = Exercise.builder().id(id3).build();

        ExerciseListItemDTO dto1 = new ExerciseListItemDTO(
            id1,
            "Exercise 1",
            "exercise-1",
            Category.STRENGTH,
            Difficulty.BEGINNER
        );

        ExerciseListItemDTO dto2 = new ExerciseListItemDTO(
            id2,
            "Exercise 2",
            "exercise-2",
            Category.STRENGTH,
            Difficulty.INTERMEDIATE
        );

        when(
            exerciseRepository.findByFilters(any(), any(), any(), any(), any())
        ).thenReturn(List.of(e1, e2, e3));

        when(exerciseMapper.toItemDTO(e1)).thenReturn(dto1);
        when(exerciseMapper.toItemDTO(e2)).thenReturn(dto2);

        var result = exerciseService.getAllExercises(
            new CursorPageRequest<>(null, 2),
            null,
            null,
            null
        );

        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).contains(id2);
        assertThat(result.page()).hasSize(2);
        assertThat(result.page()).containsExactly(dto1, dto2);
    }

    @Test
    void getAllExercises_shouldReturnPageWithoutNextCursorWhenNoMoreElementsExist() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Exercise e1 = Exercise.builder().id(id1).build();
        Exercise e2 = Exercise.builder().id(id2).build();

        ExerciseListItemDTO dto1 = new ExerciseListItemDTO(
            id1,
            "Exercise 1",
            "exercise-1",
            Category.STRENGTH,
            Difficulty.BEGINNER
        );

        ExerciseListItemDTO dto2 = new ExerciseListItemDTO(
            id2,
            "exercise 2",
            "exercise-2",
            Category.STRENGTH,
            Difficulty.INTERMEDIATE
        );

        when(
            exerciseRepository.findByFilters(any(), any(), any(), any(), any())
        ).thenReturn(List.of(e1, e2));

        when(exerciseMapper.toItemDTO(e1)).thenReturn(dto1);
        when(exerciseMapper.toItemDTO(e2)).thenReturn(dto2);

        var result = exerciseService.getAllExercises(
            new CursorPageRequest<>(null, 2),
            null,
            null,
            null
        );

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isEmpty();
        assertThat(result.page()).hasSize(2);
        assertThat(result.page()).containsExactly(dto1, dto2);
    }

    @Test
    void getExerciseBySlug_shouldReturnExerciseDetailWhenSlugExists() {
        String slug = "test-slug";
        UUID id = UUID.randomUUID();

        Exercise exercise = Exercise.builder()
            .id(id)
            .slug(slug)
            .name("Test Exercise")
            .description("Test Description")
            .category(Category.STRENGTH)
            .difficulty(Difficulty.BEGINNER)
            .build();

        ExerciseDetailDTO detailDTO = new ExerciseDetailDTO(
            id,
            "Test Exercise",
            slug,
            "Test Description",
            Category.STRENGTH,
            Difficulty.BEGINNER,
            Set.of()
        );

        when(exerciseRepository.findBySlug(slug)).thenReturn(
            Optional.of(exercise)
        );

        when(exerciseMapper.toDetailDTO(exercise)).thenReturn(detailDTO);

        var result = exerciseService.getExerciseBySlug(slug);

        assertThat(result).isNotNull();
        assertThat(result.slug()).isEqualTo(slug);
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("Test Exercise");
    }

    @Test
    void getExerciseBySlug_shouldThrowExceptionWhenExerciseDoesNotExist() {
        String slug = "test-slug";

        when(exerciseRepository.findBySlug(slug)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exerciseService.getExerciseBySlug(slug))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Exercise with slug " + slug + " not found");
    }
}
