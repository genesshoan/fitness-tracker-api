package dev.genesshoan.fitnesstrackerapi.unit;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPageRequest;
import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseService;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseDetailDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseMuscleRequestDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseRequestDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.mapper.ExerciseMapper;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.MuscleRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.Muscle;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.MuscleBuilder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private MuscleRepository muscleRepository;

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

        when(exerciseRepository.findByFiltersAndActiveTrue(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        exerciseService.getAllExercises(request, category, difficulty, muscleSlugs);

        // Then
        verify(exerciseRepository)
                .findByFiltersAndActiveTrue(
                        eq(request.cursor()), eq(category), eq(difficulty), eq(muscleSlugs), eq(request.pageable()));

        verifyNoInteractions(exerciseMapper);
    }

    @Test
    @DisplayName("Should return exercise detail when slug exists")
    void getExerciseBySlug_shouldReturnExerciseDetailWhenSlugExists() {
        // Given
        String slug = "bench-press";

        var exercise = ExerciseBuilder.anExercise(FAKER)
                .withSlug(slug)
                .withInstructions(List.of("Step 1", "Step 2"))
                .build();

        var detailDTO = new ExerciseDetailDTO(
                UUID.randomUUID(),
                exercise.getName(),
                exercise.getSlug(),
                exercise.getDescription(),
                exercise.getInstructions(),
                exercise.getCategory(),
                exercise.getDifficulty(),
                List.of(),
                null);

        when(exerciseRepository.findBySlugAndActiveTrue(slug)).thenReturn(Optional.of(exercise));

        when(exerciseMapper.toDetailDTO(exercise)).thenReturn(detailDTO);

        // When
        var result = exerciseService.getExerciseBySlug(slug);

        // Then
        assertThat(result).isEqualTo(detailDTO);

        verify(exerciseRepository).findBySlugAndActiveTrue(slug);

        verify(exerciseMapper).toDetailDTO(exercise);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when exercise does not exist")
    void getExerciseBySlug_shouldThrowExceptionWhenExerciseDoesNotExist() {
        // Given
        String slug = "bench-press";

        when(exerciseRepository.findBySlugAndActiveTrue(slug)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> exerciseService.getExerciseBySlug(slug))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Exercise with slug " + slug + " not found");

        verify(exerciseRepository).findBySlugAndActiveTrue(slug);

        verifyNoInteractions(exerciseMapper);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("Should throw BadRequestException when slug is invalid")
    void getExerciseBySlug_shouldThrowExceptionWhenSlugIsInvalid(String slug) {
        // When / Then
        assertThatThrownBy(() -> exerciseService.getExerciseBySlug(slug))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Slug is required");

        verifyNoInteractions(exerciseRepository);
        verifyNoInteractions(exerciseMapper);
    }

    @Test
    @DisplayName("Should create exercise when slug is new")
    void createExercise_shouldCreateExerciseWhenSlugIsNew() {
        // Given
        Muscle muscle = MuscleBuilder.aMuscle(FAKER).withSlug("biceps").build();

        var request = new ExerciseRequestDTO(
                "Bicep Curl",
                "bicep-curl",
                "Made with a bicep curl bar",
                List.of("Step 1", "Step 2"),
                Category.STRENGTH,
                Difficulty.INTERMEDIATE,
                List.of(new ExerciseMuscleRequestDTO("biceps", ImpactLevel.PRIMARY)));

        var exercise = ExerciseBuilder.anExercise(FAKER).build();

        var detailDTO = new ExerciseDetailDTO(
                UUID.randomUUID(),
                request.name(),
                request.slug(),
                request.description(),
                request.instructions(),
                request.category(),
                request.difficulty(),
                List.of(),
                null);

        when(exerciseRepository.existsBySlug(request.slug())).thenReturn(false);
        when(exerciseMapper.toEntity(request)).thenReturn(exercise);
        when(muscleRepository.findBySlugIn(List.of("biceps"))).thenReturn(List.of(muscle));
        when(exerciseRepository.save(exercise)).thenReturn(exercise);
        when(exerciseMapper.toDetailDTO(exercise)).thenReturn(detailDTO);

        // When
        var result = exerciseService.createExercise(request);

        // Then
        assertThat(result).isEqualTo(detailDTO);
        assertThat(exercise.isActive()).isTrue();
        assertThat(exercise.getExerciseMuscles()).hasSize(1);

        verify(exerciseRepository).existsBySlug(request.slug());
        verify(exerciseRepository).save(exercise);
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when slug is taken")
    void createExercise_shouldThrowExceptionWhenSlugExists() {
        // Given
        var request = new ExerciseRequestDTO(
                "Bicep Curl",
                "bicep-curl",
                "Made with a bicep curl bar",
                null,
                Category.STRENGTH,
                Difficulty.INTERMEDIATE,
                null);

        when(exerciseRepository.existsBySlug(request.slug())).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> exerciseService.createExercise(request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Exercise with slug bicep-curl already exists");

        verify(exerciseRepository, never()).save(any(Exercise.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when muscle does not exist")
    void createExercise_shouldThrowExceptionWhenMuscleNotFound() {
        // Given
        var request = new ExerciseRequestDTO(
                "Bicep Curl",
                "bicep-curl",
                "Made with a bicep curl bar",
                null,
                Category.STRENGTH,
                Difficulty.INTERMEDIATE,
                List.of(new ExerciseMuscleRequestDTO("unknown-muscle", ImpactLevel.PRIMARY)));

        var exercise = ExerciseBuilder.anExercise(FAKER).build();

        when(exerciseRepository.existsBySlug(request.slug())).thenReturn(false);
        when(exerciseMapper.toEntity(request)).thenReturn(exercise);
        when(muscleRepository.findBySlugIn(List.of("unknown-muscle"))).thenReturn(List.of());

        // When / Then
        assertThatThrownBy(() -> exerciseService.createExercise(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Muscles with slugs [unknown-muscle] not found");
    }

    @Test
    @DisplayName("Should update exercise when slug exists")
    void updateExercise_shouldUpdateExerciseWhenSlugExists() {
        // Given
        String slug = "bench-press";

        var exercise = ExerciseBuilder.anExercise(FAKER).withSlug(slug).build();

        Muscle muscle = MuscleBuilder.aMuscle(FAKER).withSlug("chest-upper").build();

        var request = new ExerciseRequestDTO(
                "Updated Bench Press",
                "updated-bench-press",
                "Updated description",
                List.of("Updated step"),
                Category.STRENGTH,
                Difficulty.ADVANCED,
                List.of(new ExerciseMuscleRequestDTO("chest-upper", ImpactLevel.PRIMARY)));

        var detailDTO = new ExerciseDetailDTO(
                exercise.getId(),
                request.name(),
                request.slug(),
                request.description(),
                request.instructions(),
                request.category(),
                request.difficulty(),
                List.of(),
                null);

        when(exerciseRepository.findBySlugAndActiveTrue(slug)).thenReturn(Optional.of(exercise));
        when(exerciseRepository.existsBySlug(request.slug())).thenReturn(false);
        when(muscleRepository.findBySlugIn(List.of("chest-upper"))).thenReturn(List.of(muscle));
        when(exerciseRepository.save(exercise)).thenReturn(exercise);
        when(exerciseMapper.toDetailDTO(exercise)).thenReturn(detailDTO);

        // When
        var result = exerciseService.updateExercise(slug, request);

        // Then
        assertThat(result).isEqualTo(detailDTO);
        assertThat(exercise.getName()).isEqualTo("Updated Bench Press");
        assertThat(exercise.getSlug()).isEqualTo("updated-bench-press");
        assertThat(exercise.getExerciseMuscles()).hasSize(1);

        verify(exerciseRepository).save(exercise);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating missing exercise")
    void updateExercise_shouldThrowExceptionWhenExerciseDoesNotExist() {
        // Given
        var request = new ExerciseRequestDTO(
                "Bench Press", "bench-press", "Description", null, Category.STRENGTH, Difficulty.INTERMEDIATE, null);

        when(exerciseRepository.findBySlugAndActiveTrue("bench-press")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> exerciseService.updateExercise("bench-press", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Exercise with slug bench-press not found");
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when new slug is taken")
    void updateExercise_shouldThrowExceptionWhenNewSlugExists() {
        // Given
        var exercise = ExerciseBuilder.anExercise(FAKER).withSlug("bench-press").build();

        var request = new ExerciseRequestDTO(
                "Bench Press", "taken-slug", "Description", null, Category.STRENGTH, Difficulty.INTERMEDIATE, null);

        when(exerciseRepository.findBySlugAndActiveTrue("bench-press")).thenReturn(Optional.of(exercise));
        when(exerciseRepository.existsBySlug("taken-slug")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> exerciseService.updateExercise("bench-press", request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Exercise with slug taken-slug already exists");
    }

    @Test
    @DisplayName("Should soft-delete exercise when slug exists")
    void deleteExercise_shouldSoftDeleteWhenSlugExists() {
        // Given
        when(exerciseRepository.softDeleteBySlug("bench-press")).thenReturn(1);

        // When
        exerciseService.deleteExercise("bench-press");

        // Then
        verify(exerciseRepository).softDeleteBySlug("bench-press");
    }

    @Test
    @DisplayName("Should default null instructions to empty list on create")
    void createExercise_shouldDefaultNullInstructionsToEmptyList() {
        // Given
        var request = new ExerciseRequestDTO(
                "Bicep Curl",
                "bicep-curl",
                "Made with a bicep curl bar",
                null,
                Category.STRENGTH,
                Difficulty.INTERMEDIATE,
                null);

        var exercise = ExerciseBuilder.anExercise(FAKER).build();

        var detailDTO = new ExerciseDetailDTO(
                UUID.randomUUID(),
                request.name(),
                request.slug(),
                request.description(),
                List.of(),
                request.category(),
                request.difficulty(),
                List.of(),
                null);

        when(exerciseRepository.existsBySlug(request.slug())).thenReturn(false);
        when(exerciseMapper.toEntity(request)).thenReturn(exercise);
        when(exerciseRepository.save(exercise)).thenReturn(exercise);
        when(exerciseMapper.toDetailDTO(exercise)).thenReturn(detailDTO);

        // When
        var result = exerciseService.createExercise(request);

        // Then
        assertThat(result).isEqualTo(detailDTO);
        assertThat(exercise.getInstructions()).isEmpty();
    }

    @Test
    @DisplayName("Should throw BadRequestException when muscles are duplicated")
    void createExercise_shouldThrowExceptionWhenMusclesAreDuplicated() {
        // Given
        var request = new ExerciseRequestDTO(
                "Bicep Curl",
                "bicep-curl",
                "Made with a bicep curl bar",
                null,
                Category.STRENGTH,
                Difficulty.INTERMEDIATE,
                List.of(
                        new ExerciseMuscleRequestDTO("biceps", ImpactLevel.PRIMARY),
                        new ExerciseMuscleRequestDTO("biceps", ImpactLevel.SECONDARY)));

        var exercise = ExerciseBuilder.anExercise(FAKER).build();

        when(exerciseRepository.existsBySlug(request.slug())).thenReturn(false);
        when(exerciseMapper.toEntity(request)).thenReturn(exercise);

        // When / Then
        assertThatThrownBy(() -> exerciseService.createExercise(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Duplicate muscles in request: [biceps]");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting missing exercise")
    void deleteExercise_shouldThrowExceptionWhenExerciseDoesNotExist() {
        // Given
        when(exerciseRepository.softDeleteBySlug("bench-press")).thenReturn(0);

        // When / Then
        assertThatThrownBy(() -> exerciseService.deleteExercise("bench-press"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Exercise with slug bench-press not found");
    }
}
