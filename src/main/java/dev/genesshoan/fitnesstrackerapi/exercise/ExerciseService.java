package dev.genesshoan.fitnesstrackerapi.exercise;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPage;
import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPageRequest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseDetailDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseListItemDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.mapper.ExerciseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for exercise catalog operations.
 *
 * <p>Provides read-only access to the exercise catalog with filtering
 * and cursor-based pagination. All methods are read-only transactions.
 *
 * <p>Exercises are filtered by:
 * <ul>
 *   <li>Active status (only active exercises are returned)</li>
 *   <li>Category (strength, cardio, mobility)</li>
 *   <li>Difficulty (beginner, intermediate, advanced)</li>
 *   <li>Muscle slugs (one or more target muscles)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseMapper exerciseMapper;

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
    public CursorPage<ExerciseListItemDTO, UUID> getAllExercises(
            CursorPageRequest<UUID> request, Category category, Difficulty difficulty, List<String> muscleSlugs) {
        List<Exercise> exercises = exerciseRepository.findByFiltersAndActiveTrue(
                request.cursor(), category, difficulty, muscleSlugs, request.pageable());

        log.info("Found {} exercises", exercises == null ? 0 : exercises.size());

        return CursorPage.of(exercises, request.size(), Exercise::getId, exerciseMapper::toItemDTO);
    }

    /**
     * Retrieves a single active exercise by its slug.
     *
     * <p>The slug is a URL-friendly identifier (e.g., "bicep-curl").
     * Only exercises with {@code active = true} are returned.
     *
     * <p>The returned {@link ExerciseDetailDTO} includes the full
     * exercise-muscle relationships with impact levels.
     *
     * @param slug the URL-friendly exercise identifier
     * @return a {@link ExerciseDetailDTO} with full exercise details
     * @throws BadRequestException if the slug is null or blank
     * @throws ResourceNotFoundException if no active exercise exists with the given slug
     */
    public ExerciseDetailDTO getExerciseBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new BadRequestException("Slug is required");
        }

        var exercise = exerciseRepository.findBySlugAndActiveTrue(slug).orElseThrow(() -> {
            log.warn("Exercise with slug {} not found", slug);
            return new ResourceNotFoundException("Exercise with slug " + slug + " not found");
        });

        log.info("Found exercise with slug {}", slug);

        return exerciseMapper.toDetailDTO(exercise);
    }
}
