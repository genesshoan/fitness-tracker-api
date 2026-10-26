package dev.genesshoan.fitnesstrackerapi.exercise.application.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPage;
import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPageRequest;
import dev.genesshoan.fitnesstrackerapi.exercise.application.mapper.ExerciseResultMapper;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.command.ExerciseCommand;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.command.ExerciseMuscleCommand;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.ExerciseDetailResult;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.ExerciseListItemResult;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.ExerciseSuggestion;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Muscle;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.persistence.ExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.persistence.MuscleRepository;
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
    private ExerciseResultMapper exerciseResultMapper;

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

        verifyNoInteractions(exerciseResultMapper);
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

        var detail = new ExerciseDetailResult(
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

        when(exerciseResultMapper.toDetail(exercise)).thenReturn(detail);

        // When
        var result = exerciseService.getExerciseBySlug(slug);

        // Then
        assertThat(result).isEqualTo(detail);

        verify(exerciseRepository).findBySlugAndActiveTrue(slug);

        verify(exerciseResultMapper).toDetail(exercise);
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

        verifyNoInteractions(exerciseResultMapper);
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
        verifyNoInteractions(exerciseResultMapper);
    }

    @Test
    @DisplayName("Should create exercise when slug is new")
    void createExercise_shouldCreateExerciseWhenSlugIsNew() {
        // Given
        Muscle muscle = MuscleBuilder.aMuscle(FAKER).withSlug("biceps").build();

        var command = new ExerciseCommand(
                "Bicep Curl",
                "bicep-curl",
                "Made with a bicep curl bar",
                List.of("Step 1", "Step 2"),
                Category.STRENGTH,
                Difficulty.INTERMEDIATE,
                List.of(new ExerciseMuscleCommand("biceps", ImpactLevel.PRIMARY)));

        var exercise = ExerciseBuilder.anExercise(FAKER).build();

        var detail = new ExerciseDetailResult(
                UUID.randomUUID(),
                command.name(),
                command.slug(),
                command.description(),
                command.instructions(),
                command.category(),
                command.difficulty(),
                List.of(),
                null);

        when(exerciseRepository.existsBySlug(command.slug())).thenReturn(false);
        when(exerciseResultMapper.toEntity(command)).thenReturn(exercise);
        when(muscleRepository.findBySlugIn(List.of("biceps"))).thenReturn(List.of(muscle));
        when(exerciseRepository.save(exercise)).thenReturn(exercise);
        when(exerciseResultMapper.toDetail(exercise)).thenReturn(detail);

        // When
        var result = exerciseService.createExercise(command);

        // Then
        assertThat(result).isEqualTo(detail);
        assertThat(exercise.isActive()).isTrue();
        assertThat(exercise.getExerciseMuscles()).hasSize(1);

        verify(exerciseRepository).existsBySlug(command.slug());
        verify(exerciseRepository).save(exercise);
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when slug is taken")
    void createExercise_shouldThrowExceptionWhenSlugExists() {
        // Given
        var command = new ExerciseCommand(
                "Bicep Curl",
                "bicep-curl",
                "Made with a bicep curl bar",
                null,
                Category.STRENGTH,
                Difficulty.INTERMEDIATE,
                null);

        when(exerciseRepository.existsBySlug(command.slug())).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> exerciseService.createExercise(command))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Exercise with slug bicep-curl already exists");

        verify(exerciseRepository, never()).save(any(Exercise.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when muscle does not exist")
    void createExercise_shouldThrowExceptionWhenMuscleNotFound() {
        // Given
        var command = new ExerciseCommand(
                "Bicep Curl",
                "bicep-curl",
                "Made with a bicep curl bar",
                null,
                Category.STRENGTH,
                Difficulty.INTERMEDIATE,
                List.of(new ExerciseMuscleCommand("unknown-muscle", ImpactLevel.PRIMARY)));

        var exercise = ExerciseBuilder.anExercise(FAKER).build();

        when(exerciseRepository.existsBySlug(command.slug())).thenReturn(false);
        when(exerciseResultMapper.toEntity(command)).thenReturn(exercise);
        when(muscleRepository.findBySlugIn(List.of("unknown-muscle"))).thenReturn(List.of());

        // When / Then
        assertThatThrownBy(() -> exerciseService.createExercise(command))
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

        var command = new ExerciseCommand(
                "Updated Bench Press",
                "updated-bench-press",
                "Updated description",
                List.of("Updated step"),
                Category.STRENGTH,
                Difficulty.ADVANCED,
                List.of(new ExerciseMuscleCommand("chest-upper", ImpactLevel.PRIMARY)));

        var detail = new ExerciseDetailResult(
                exercise.getId(),
                command.name(),
                command.slug(),
                command.description(),
                command.instructions(),
                command.category(),
                command.difficulty(),
                List.of(),
                null);

        when(exerciseRepository.findBySlugAndActiveTrue(slug)).thenReturn(Optional.of(exercise));
        when(exerciseRepository.existsBySlug(command.slug())).thenReturn(false);
        when(muscleRepository.findBySlugIn(List.of("chest-upper"))).thenReturn(List.of(muscle));
        when(exerciseRepository.save(exercise)).thenReturn(exercise);
        when(exerciseResultMapper.toDetail(exercise)).thenReturn(detail);

        // When
        var result = exerciseService.updateExercise(slug, command);

        // Then
        assertThat(result).isEqualTo(detail);
        assertThat(exercise.getName()).isEqualTo("Updated Bench Press");
        assertThat(exercise.getSlug()).isEqualTo("updated-bench-press");
        assertThat(exercise.getExerciseMuscles()).hasSize(1);

        verify(exerciseRepository).save(exercise);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating missing exercise")
    void updateExercise_shouldThrowExceptionWhenExerciseDoesNotExist() {
        // Given
        var command = new ExerciseCommand(
                "Bench Press", "bench-press", "Description", null, Category.STRENGTH, Difficulty.INTERMEDIATE, null);

        when(exerciseRepository.findBySlugAndActiveTrue("bench-press")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> exerciseService.updateExercise("bench-press", command))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Exercise with slug bench-press not found");
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when new slug is taken")
    void updateExercise_shouldThrowExceptionWhenNewSlugExists() {
        // Given
        var exercise = ExerciseBuilder.anExercise(FAKER).withSlug("bench-press").build();

        var command = new ExerciseCommand(
                "Bench Press", "taken-slug", "Description", null, Category.STRENGTH, Difficulty.INTERMEDIATE, null);

        when(exerciseRepository.findBySlugAndActiveTrue("bench-press")).thenReturn(Optional.of(exercise));
        when(exerciseRepository.existsBySlug("taken-slug")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> exerciseService.updateExercise("bench-press", command))
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
        var command = new ExerciseCommand(
                "Bicep Curl",
                "bicep-curl",
                "Made with a bicep curl bar",
                null,
                Category.STRENGTH,
                Difficulty.INTERMEDIATE,
                null);

        var exercise = ExerciseBuilder.anExercise(FAKER).build();

        var detail = new ExerciseDetailResult(
                UUID.randomUUID(),
                command.name(),
                command.slug(),
                command.description(),
                List.of(),
                command.category(),
                command.difficulty(),
                List.of(),
                null);

        when(exerciseRepository.existsBySlug(command.slug())).thenReturn(false);
        when(exerciseResultMapper.toEntity(command)).thenReturn(exercise);
        when(exerciseRepository.save(exercise)).thenReturn(exercise);
        when(exerciseResultMapper.toDetail(exercise)).thenReturn(detail);

        // When
        var result = exerciseService.createExercise(command);

        // Then
        assertThat(result).isEqualTo(detail);
        assertThat(exercise.getInstructions()).isEmpty();
    }

    @Test
    @DisplayName("Should throw BadRequestException when muscles are duplicated")
    void createExercise_shouldThrowExceptionWhenMusclesAreDuplicated() {
        // Given
        var command = new ExerciseCommand(
                "Bicep Curl",
                "bicep-curl",
                "Made with a bicep curl bar",
                null,
                Category.STRENGTH,
                Difficulty.INTERMEDIATE,
                List.of(
                        new ExerciseMuscleCommand("biceps", ImpactLevel.PRIMARY),
                        new ExerciseMuscleCommand("biceps", ImpactLevel.SECONDARY)));

        var exercise = ExerciseBuilder.anExercise(FAKER).build();

        when(exerciseRepository.existsBySlug(command.slug())).thenReturn(false);
        when(exerciseResultMapper.toEntity(command)).thenReturn(exercise);

        // When / Then
        assertThatThrownBy(() -> exerciseService.createExercise(command))
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

    @Test
    @DisplayName("Should map the fetched exercises to list items and expose the next cursor")
    void getAllExercises_shouldMapExercisesToListItemsAndExposeNextCursor() {
        // Given
        var first = ExerciseBuilder.anExercise(FAKER).withSlug("first-exercise").build();
        var second =
                ExerciseBuilder.anExercise(FAKER).withSlug("second-exercise").build();

        var firstItem = new ExerciseListItemResult(
                first.getId(), first.getName(), first.getSlug(), first.getCategory(), first.getDifficulty(), null);

        CursorPageRequest<UUID> request = new CursorPageRequest<>(null, 1);

        when(exerciseRepository.findByFiltersAndActiveTrue(any(), any(), any(), any(), any()))
                .thenReturn(List.of(first, second));
        when(exerciseResultMapper.toListItem(first)).thenReturn(firstItem);

        // When
        CursorPage<ExerciseListItemResult, UUID> page = exerciseService.getAllExercises(request, null, null, null);

        // Then
        assertThat(page.page()).containsExactly(firstItem);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).contains(first.getId());
    }

    @Test
    @DisplayName("Should return the matching exercises with the query highlighted in the name")
    void searchExercises_shouldReturnMatchingExercisesWithHighlightedName() {
        // Given
        var exercise = ExerciseBuilder.anExercise(FAKER)
                .withName("Barbell Bench Press")
                .withSlug("barbell-bench-press")
                .build();

        when(exerciseRepository.searchActive("barbell", 10)).thenReturn(List.of(exercise));

        // When
        var result = exerciseService.searchExercises("barbell", 10);

        // Then
        assertThat(result)
                .containsExactly(new ExerciseSuggestion(
                        exercise.getId(),
                        "Barbell Bench Press",
                        "barbell-bench-press",
                        exercise.getCategory(),
                        "<b>Barbell</b> Bench Press"));
    }

    @Test
    @DisplayName("Should return an empty result when no exercise matches the query")
    void searchExercises_shouldReturnEmptyResultsWhenNothingMatches() {
        // Given
        when(exerciseRepository.searchActive("press", 10)).thenReturn(List.of());

        // When
        var result = exerciseService.searchExercises("press", 10);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should trim the query before searching")
    void searchExercises_shouldTrimTheQueryBeforeSearching() {
        // Given
        when(exerciseRepository.searchActive("press", 5)).thenReturn(List.of());

        // When
        exerciseService.searchExercises("  press  ", 5);

        // Then
        verify(exerciseRepository).searchActive("press", 5);
    }

    @Test
    @DisplayName("Should search with an empty query when the query is null")
    void searchExercises_shouldSearchWithAnEmptyQueryWhenQueryIsNull() {
        // Given
        when(exerciseRepository.searchActive("", 5)).thenReturn(List.of());

        // When
        var result = exerciseService.searchExercises(null, 5);

        // Then
        verify(exerciseRepository).searchActive("", 5);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should escape LIKE metacharacters in the search query")
    void searchExercises_shouldEscapeLikeMetacharactersInTheQuery() {
        // Given
        when(exerciseRepository.searchActive("100\\% effort", 10)).thenReturn(List.of());
        when(exerciseRepository.searchActive("a\\_b", 10)).thenReturn(List.of());

        // When
        exerciseService.searchExercises("100% effort", 10);
        exerciseService.searchExercises("a_b", 10);

        // Then
        verify(exerciseRepository).searchActive("100\\% effort", 10);
        verify(exerciseRepository).searchActive("a\\_b", 10);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("Should throw BadRequestException when deleting with an invalid slug")
    void deleteExercise_shouldThrowExceptionWhenSlugIsInvalid(String slug) {
        // When / Then
        assertThatThrownBy(() -> exerciseService.deleteExercise(slug))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Slug is required");

        verifyNoInteractions(exerciseRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("Should throw BadRequestException when updating with an invalid slug")
    void updateExercise_shouldThrowExceptionWhenSlugIsInvalid(String slug) {
        // Given
        var command = new ExerciseCommand(
                "Bench Press", "bench-press", "Description", null, Category.STRENGTH, Difficulty.INTERMEDIATE, null);

        // When / Then
        assertThatThrownBy(() -> exerciseService.updateExercise(slug, command))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Slug is required");

        verifyNoInteractions(exerciseRepository);
    }

    @Test
    @DisplayName("Should create an exercise without muscle associations")
    void createExercise_shouldCreateExerciseWithoutMuscles() {
        // Given
        var command = new ExerciseCommand(
                "Bicep Curl",
                "bicep-curl",
                "Made with a bicep curl bar",
                List.of("Step 1"),
                Category.STRENGTH,
                Difficulty.INTERMEDIATE,
                null);

        var exercise = ExerciseBuilder.anExercise(FAKER).build();

        var detail = new ExerciseDetailResult(
                UUID.randomUUID(),
                command.name(),
                command.slug(),
                command.description(),
                command.instructions(),
                command.category(),
                command.difficulty(),
                List.of(),
                null);

        when(exerciseRepository.existsBySlug(command.slug())).thenReturn(false);
        when(exerciseResultMapper.toEntity(command)).thenReturn(exercise);
        when(exerciseRepository.save(exercise)).thenReturn(exercise);
        when(exerciseResultMapper.toDetail(exercise)).thenReturn(detail);

        // When
        var result = exerciseService.createExercise(command);

        // Then
        assertThat(result).isEqualTo(detail);
        assertThat(exercise.getExerciseMuscles()).isEmpty();

        verifyNoInteractions(muscleRepository);
    }

    @Test
    @DisplayName("Should check whether an active exercise exists")
    void existsById_shouldDelegateToTheRepository() {
        // Given
        UUID id = UUID.randomUUID();

        when(exerciseRepository.existsByIdAndActiveTrue(id)).thenReturn(true);

        // When / Then
        assertThat(exerciseService.existsById(id)).isTrue();

        verify(exerciseRepository).existsByIdAndActiveTrue(id);
    }

    @Test
    @DisplayName("Should find an active exercise by id")
    void findById_shouldReturnTheActiveExercise() {
        // Given
        UUID id = UUID.randomUUID();
        var exercise = ExerciseBuilder.anExercise(FAKER).build();

        when(exerciseRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.of(exercise));

        // When / Then
        assertThat(exerciseService.findById(id)).contains(exercise);

        verify(exerciseRepository).findByIdAndActiveTrue(id);
    }

    @Test
    @DisplayName("Should return the found exercises keyed by id")
    void findByIds_shouldReturnTheFoundExercisesKeyedById() {
        // Given
        var first = ExerciseBuilder.anExercise(FAKER).build();
        var second = ExerciseBuilder.anExercise(FAKER).build();

        when(exerciseRepository.findAllByIdInAndActiveTrue(any())).thenReturn(List.of(first, second));

        // When
        var result = exerciseService.findByIds(List.of(first.getId(), second.getId()));

        // Then
        assertThat(result).containsOnlyKeys(first.getId(), second.getId());
    }

    @Test
    @DisplayName("Should return an empty map when no ids are requested")
    void findByIds_shouldReturnAnEmptyMapWhenNoIdsAreRequested() {
        // When / Then
        assertThat(exerciseService.findByIds(List.of())).isEmpty();

        verifyNoInteractions(exerciseRepository);
    }
}
