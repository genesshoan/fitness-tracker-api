package dev.genesshoan.fitnesstrackerapi.exercise;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPage;
import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPageRequest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ExerciseMuscle;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ExerciseMuscleId;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseDetailDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseListItemDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseMuscleRequestDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseRequestDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.mapper.ExerciseMapper;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.MuscleRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.Muscle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for exercise catalog operations.
 *
 * <p>Provides read access to the exercise catalog with filtering
 * and cursor-based pagination, plus ADMIN-only write operations
 * (create, full update and soft delete). Read methods are read-only
 * transactions; write methods open read-write transactions.
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
    private final MuscleRepository muscleRepository;
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
        Exercise exercise = findActiveBySlug(slug);

        log.info("Found exercise with slug {}", slug);

        return exerciseMapper.toDetailDTO(exercise);
    }

    /**
     * Creates a new exercise from the given request.
     *
     * <p>The slug must be unique across all exercises, including inactive
     * ones. Muscle slugs must reference existing muscles.
     *
     * @param request the exercise data
     * @return an {@link ExerciseDetailDTO} with the created exercise
     * @throws ResourceAlreadyExistsException if another exercise already uses the slug
     * @throws ResourceNotFoundException if a referenced muscle does not exist
     */
    @Transactional
    public ExerciseDetailDTO createExercise(ExerciseRequestDTO request) {
        if (exerciseRepository.existsBySlug(request.slug())) {
            throw new ResourceAlreadyExistsException("Exercise with slug " + request.slug() + " already exists");
        }

        Exercise exercise = exerciseMapper.toEntity(request);
        exercise.setActive(true);
        exercise.setInstructions(orEmptyInstructions(request.instructions()));
        exercise.setExerciseMuscles(resolveMuscles(exercise, request.muscles()));

        Exercise saved = exerciseRepository.save(exercise);

        log.info("Created exercise with slug {}", saved.getSlug());

        return exerciseMapper.toDetailDTO(saved);
    }

    /**
     * Fully updates the active exercise identified by the given slug.
     *
     * <p>Supplied muscles replace the exercise's current muscle associations.
     * The slug itself may be changed as long as the new value is not taken.
     *
     * @param slug the URL-friendly identifier of the exercise to update
     * @param request the new exercise data
     * @return an {@link ExerciseDetailDTO} with the updated exercise
     * @throws BadRequestException if the slug is null or blank
     * @throws ResourceNotFoundException if no active exercise exists with the given slug,
     *                                   or a referenced muscle does not exist
     * @throws ResourceAlreadyExistsException if the new slug is already used by another exercise
     */
    @Transactional
    public ExerciseDetailDTO updateExercise(String slug, ExerciseRequestDTO request) {
        Exercise exercise = findActiveBySlug(slug);

        if (!exercise.getSlug().equals(request.slug()) && exerciseRepository.existsBySlug(request.slug())) {
            throw new ResourceAlreadyExistsException("Exercise with slug " + request.slug() + " already exists");
        }

        exercise.setName(request.name());
        exercise.setSlug(request.slug());
        exercise.setDescription(request.description());
        exercise.setInstructions(orEmptyInstructions(request.instructions()));
        exercise.setCategory(request.category());
        exercise.setDifficulty(request.difficulty());
        exercise.getExerciseMuscles().clear();
        exercise.getExerciseMuscles().addAll(resolveMuscles(exercise, request.muscles()));

        Exercise saved = exerciseRepository.save(exercise);

        log.info("Updated exercise with slug {}", saved.getSlug());

        return exerciseMapper.toDetailDTO(saved);
    }

    /**
     * Soft-deletes the active exercise identified by the given slug.
     *
     * <p>The exercise is kept in the database with {@code active = false} and
     * is excluded from all read operations. Implemented as a direct update
     * query: the entity (and its muscle graph) is never loaded.
     *
     * @param slug the URL-friendly identifier of the exercise to delete
     * @throws BadRequestException if the slug is null or blank
     * @throws ResourceNotFoundException if no active exercise exists with the given slug
     */
    @Transactional
    public void deleteExercise(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new BadRequestException("Slug is required");
        }

        int updated = exerciseRepository.softDeleteBySlug(slug);

        if (updated == 0) {
            log.warn("Exercise with slug {} not found", slug);
            throw new ResourceNotFoundException("Exercise with slug " + slug + " not found");
        }

        log.info("Soft-deleted exercise with slug {}", slug);
    }

    private Exercise findActiveBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new BadRequestException("Slug is required");
        }

        return exerciseRepository.findBySlugAndActiveTrue(slug).orElseThrow(() -> {
            log.warn("Exercise with slug {} not found", slug);
            return new ResourceNotFoundException("Exercise with slug " + slug + " not found");
        });
    }

    private List<String> orEmptyInstructions(List<String> instructions) {
        return instructions == null ? List.of() : instructions;
    }

    private Set<ExerciseMuscle> resolveMuscles(Exercise exercise, List<ExerciseMuscleRequestDTO> muscles) {
        if (muscles == null || muscles.isEmpty()) {
            return new HashSet<>();
        }

        List<String> slugs =
                muscles.stream().map(ExerciseMuscleRequestDTO::muscleSlug).toList();

        Set<String> seen = new HashSet<>();
        List<String> duplicates =
                slugs.stream().filter(slug -> !seen.add(slug)).distinct().toList();

        if (!duplicates.isEmpty()) {
            throw new BadRequestException("Duplicate muscles in request: " + duplicates);
        }

        Map<String, Muscle> musclesBySlug = muscleRepository.findBySlugIn(slugs).stream()
                .collect(Collectors.toMap(Muscle::getSlug, Function.identity()));

        List<String> missing =
                slugs.stream().filter(slug -> !musclesBySlug.containsKey(slug)).toList();

        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException("Muscles with slugs " + missing + " not found");
        }

        return muscles.stream()
                .map(link -> {
                    Muscle muscle = musclesBySlug.get(link.muscleSlug());

                    return ExerciseMuscle.builder()
                            .id(new ExerciseMuscleId(exercise.getId(), muscle.getId()))
                            .exercise(exercise)
                            .muscle(muscle)
                            .impactLevel(link.impactLevel())
                            .build();
                })
                .collect(Collectors.toSet());
    }
}
