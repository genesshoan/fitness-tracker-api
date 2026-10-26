package dev.genesshoan.fitnesstrackerapi.exercise.application.port.in;

import java.util.List;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPage;
import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPageRequest;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.command.ExerciseCommand;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.ExerciseDetailResult;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.ExerciseListItemResult;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.ExerciseSuggestion;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;

/**
 * Use cases for browsing and maintaining the exercise catalog.
 *
 * <p>Reads are limited to active exercises; writes are reserved for administrators.
 * The use case returns only the data each caller needs, never the persisted entity.
 */
public interface ExerciseUseCase {

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
     * @return a {@link CursorPage} containing the list items of the matching exercises
     */
    CursorPage<ExerciseListItemResult, UUID> getAllExercises(
            CursorPageRequest<UUID> request, Category category, Difficulty difficulty, List<String> muscleSlugs);

    /**
     * Searches active exercises for autocomplete suggestions.
     *
     * <p>The query matches exercise names and slugs case-insensitively. Results
     * are ordered by name and limited by the requested result count.
     *
     * @param query text to search for
     * @param limit maximum number of suggestions
     * @return the matching suggestions, empty when nothing matches
     */
    List<ExerciseSuggestion> searchExercises(String query, int limit);

    /**
     * Retrieves a single active exercise by its slug.
     *
     * <p>The slug is a URL-friendly identifier (e.g., "bicep-curl").
     * Only exercises with {@code active = true} are returned.
     *
     * @param slug the URL-friendly exercise identifier
     * @return the details of the matching exercise
     * @throws BadRequestException if the slug is null or blank
     * @throws ResourceNotFoundException if no active exercise exists with the given slug
     */
    ExerciseDetailResult getExerciseBySlug(String slug);

    /**
     * Creates a new exercise from the given command.
     *
     * <p>The slug must be unique across all exercises, including inactive
     * ones. Muscle slugs must reference existing muscles.
     *
     * @param command the exercise data
     * @return the details of the created exercise
     * @throws ResourceAlreadyExistsException if another exercise already uses the slug
     * @throws ResourceNotFoundException if a referenced muscle does not exist
     */
    ExerciseDetailResult createExercise(ExerciseCommand command);

    /**
     * Fully updates the active exercise identified by the given slug.
     *
     * <p>Supplied muscles replace the exercise's current muscle associations.
     * The slug itself may be changed as long as the new value is not taken.
     *
     * @param slug the URL-friendly identifier of the exercise to update
     * @param command the new exercise data
     * @return the details of the updated exercise
     * @throws BadRequestException if the slug is null or blank
     * @throws ResourceNotFoundException if no active exercise exists with the given slug,
     *                                   or a referenced muscle does not exist
     * @throws ResourceAlreadyExistsException if the new slug is already used by another exercise
     */
    ExerciseDetailResult updateExercise(String slug, ExerciseCommand command);

    /**
     * Soft-deletes the active exercise identified by the given slug.
     *
     * <p>The exercise is kept in the database with {@code active = false} and
     * is excluded from all read operations.
     *
     * @param slug the URL-friendly identifier of the exercise to delete
     * @throws BadRequestException if the slug is null or blank
     * @throws ResourceNotFoundException if no active exercise exists with the given slug
     */
    void deleteExercise(String slug);
}
