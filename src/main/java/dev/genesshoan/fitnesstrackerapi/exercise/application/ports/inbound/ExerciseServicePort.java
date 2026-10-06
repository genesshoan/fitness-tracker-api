package dev.genesshoan.fitnesstrackerapi.exercise.application.ports.inbound;

import java.util.List;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPage;
import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPageRequest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseDetailDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseListItemDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseRequestDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseSearchResponseDTO;

/**
 * Application port for exercise catalog use cases.
 *
 * <p>Defines the inbound contract that controllers and other application
 * components depend on. Implementations provide the actual business logic.
 */
public interface ExerciseServicePort {

    /**
     * Retrieves a paginated list of active exercises with optional filtering.
     *
     * <p>Filters are combined with AND logic. Each filter is optional;
     * null or absent filters are ignored. Only exercises with
     * {@code active = true} are returned.
     *
     * <p>Pagination uses cursor-based pagination: the cursor is the
     * last ID from the previous page. A null cursor returns the first page.
     *
     * @param request      the cursor pagination request (cursor + size)
     * @param category     optional exercise category filter; null to ignore
     * @param difficulty   optional exercise difficulty filter; null to ignore
     * @param muscleSlugs  optional list of muscle slugs to filter by;
     *                     exercises matching any of these muscles are returned
     * @return a {@link CursorPage} containing {@link ExerciseListItemDTO} items
     */
    CursorPage<ExerciseListItemDTO, UUID> getAllExercises(
            CursorPageRequest<UUID> request, Category category, Difficulty difficulty, List<String> muscleSlugs);

    /**
     * Searches active exercises for autocomplete suggestions.
     */
    ExerciseSearchResponseDTO searchExercises(String query, int limit);

    /**
     * Retrieves a single active exercise by its slug.
     */
    ExerciseDetailDTO getExerciseBySlug(String slug);

    /**
     * Creates a new exercise from the given request.
     */
    ExerciseDetailDTO createExercise(ExerciseRequestDTO request);

    /**
     * Fully updates the active exercise identified by the given slug.
     */
    ExerciseDetailDTO updateExercise(String slug, ExerciseRequestDTO request);

    /**
     * Soft-deletes the active exercise identified by the given slug.
     */
    void deleteExercise(String slug);
}
