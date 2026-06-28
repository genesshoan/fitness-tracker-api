package dev.genesshoan.fitnesstrackerapi.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPageRequest;
import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseService;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseDetailDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.mapper.ExerciseMapper;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ExerciseBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private ExerciseMapper exerciseMapper;

    @InjectMocks
    private ExerciseService exerciseService;

    @Test
    @DisplayName("Should pass filters to repository")
    void getAllExercises_shouldPassFiltersToRepository() {
        // Given
        Category category = Category.STRENGTH;
        Difficulty difficulty = Difficulty.BEGINNER;
        List<String> muscleSlugs = List.of("chest", "triceps");

        CursorPageRequest<UUID> request = new CursorPageRequest<>(null, 10);

        when(
            exerciseRepository.findByFilters(any(), any(), any(), any(), any())
        ).thenReturn(Collections.emptyList());

        // When
        exerciseService.getAllExercises(
            request,
            category,
            difficulty,
            muscleSlugs
        );

        // Then
        verify(exerciseRepository).findByFilters(
            eq(request.cursor()),
            eq(category),
            eq(difficulty),
            eq(muscleSlugs),
            eq(request.pageable())
        );

        verifyNoInteractions(exerciseMapper);
    }

    @Test
    @DisplayName("Should return exercise detail when slug exists")
    void getExerciseBySlug_shouldReturnExerciseDetailWhenSlugExists() {
        // Given
        String slug = "bench-press";

        var exercise = ExerciseBuilder.anExercise(FAKER).withSlug(slug).build();

        var detailDTO = new ExerciseDetailDTO(
            UUID.randomUUID(),
            exercise.getName(),
            exercise.getSlug(),
            exercise.getDescription(),
            exercise.getCategory(),
            exercise.getDifficulty(),
            List.of()
        );

        when(exerciseRepository.findBySlugAndActiveTrue(slug)).thenReturn(
            Optional.of(exercise)
        );

        when(exerciseMapper.toDetailDTO(exercise)).thenReturn(detailDTO);

        // When
        var result = exerciseService.getExerciseBySlug(slug);

        // Then
        assertThat(result).isEqualTo(detailDTO);

        verify(exerciseRepository).findBySlugAndActiveTrue(slug);

        verify(exerciseMapper).toDetailDTO(exercise);
    }

    @Test
    @DisplayName(
        "Should throw ResourceNotFoundException when exercise does not exist"
    )
    void getExerciseBySlug_shouldThrowExceptionWhenExerciseDoesNotExist() {
        // Given
        String slug = "bench-press";

        when(exerciseRepository.findBySlugAndActiveTrue(slug)).thenReturn(
            Optional.empty()
        );

        // When / Then
        assertThatThrownBy(() -> exerciseService.getExerciseBySlug(slug))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Exercise with slug " + slug + " not found");

        verify(exerciseRepository).findBySlugAndActiveTrue(slug);

        verifyNoInteractions(exerciseMapper);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "   " })
    @DisplayName("Should throw BadRequestException when slug is invalid")
    void getExerciseBySlug_shouldThrowExceptionWhenSlugIsInvalid(String slug) {
        // When / Then
        assertThatThrownBy(() -> exerciseService.getExerciseBySlug(slug))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Slug is required");

        verifyNoInteractions(exerciseRepository);
        verifyNoInteractions(exerciseMapper);
    }
}
