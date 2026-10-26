package dev.genesshoan.fitnesstrackerapi.exercise.application.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import dev.genesshoan.fitnesstrackerapi.exercise.application.mapper.ExerciseResultMapper;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.ExerciseQueryUseCase;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.ExerciseUseCase;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.command.ExerciseCommand;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.command.ExerciseMuscleCommand;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.ExerciseDetailResult;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.ExerciseListItemResult;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.ExerciseSuggestion;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ExerciseMuscle;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ExerciseMuscleId;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Muscle;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.persistence.ExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.persistence.MuscleRepository;
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
public class ExerciseService implements ExerciseUseCase, ExerciseQueryUseCase {

    private final ExerciseRepository exerciseRepository;
    private final MuscleRepository muscleRepository;
    private final ExerciseResultMapper exerciseResultMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public CursorPage<ExerciseListItemResult, UUID> getAllExercises(
            CursorPageRequest<UUID> request, Category category, Difficulty difficulty, List<String> muscleSlugs) {
        List<Exercise> exercises = exerciseRepository.findByFiltersAndActiveTrue(
                request.cursor(), category, difficulty, muscleSlugs, request.pageable());

        log.info("Found {} exercises", exercises == null ? 0 : exercises.size());

        return CursorPage.of(exercises, request.size(), Exercise::getId, exerciseResultMapper::toListItem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExerciseSuggestion> searchExercises(String query, int limit) {
        String normalizedQuery = query == null ? "" : query.trim();

        return exerciseRepository.searchActive(escapeLike(normalizedQuery), limit).stream()
                .map(exercise -> toSuggestion(exercise, normalizedQuery))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExerciseDetailResult getExerciseBySlug(String slug) {
        Exercise exercise = findActiveBySlug(slug);

        log.info("Found exercise with slug {}", slug);

        return exerciseResultMapper.toDetail(exercise);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ExerciseDetailResult createExercise(ExerciseCommand command) {
        if (exerciseRepository.existsBySlug(command.slug())) {
            throw new ResourceAlreadyExistsException("Exercise with slug " + command.slug() + " already exists");
        }

        Exercise exercise = exerciseResultMapper.toEntity(command);
        exercise.setActive(true);
        exercise.setInstructions(orEmptyInstructions(command.instructions()));
        exercise.setExerciseMuscles(resolveMuscles(exercise, command.muscles()));

        Exercise saved = exerciseRepository.save(exercise);

        log.info("Created exercise with slug {}", saved.getSlug());

        return exerciseResultMapper.toDetail(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ExerciseDetailResult updateExercise(String slug, ExerciseCommand command) {
        Exercise exercise = findActiveBySlug(slug);

        if (!exercise.getSlug().equals(command.slug()) && exerciseRepository.existsBySlug(command.slug())) {
            throw new ResourceAlreadyExistsException("Exercise with slug " + command.slug() + " already exists");
        }

        exercise.setName(command.name());
        exercise.setSlug(command.slug());
        exercise.setDescription(command.description());
        exercise.setInstructions(orEmptyInstructions(command.instructions()));
        exercise.setCategory(command.category());
        exercise.setDifficulty(command.difficulty());
        exercise.getExerciseMuscles().clear();
        exercise.getExerciseMuscles().addAll(resolveMuscles(exercise, command.muscles()));

        Exercise saved = exerciseRepository.save(exercise);

        log.info("Updated exercise with slug {}", saved.getSlug());

        return exerciseResultMapper.toDetail(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsById(UUID id) {
        return exerciseRepository.existsByIdAndActiveTrue(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Exercise> findById(UUID id) {
        return exerciseRepository.findByIdAndActiveTrue(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<UUID, Exercise> findByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        return exerciseRepository.findAllByIdInAndActiveTrue(Set.copyOf(ids)).stream()
                .collect(Collectors.toMap(Exercise::getId, Function.identity()));
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

    private ExerciseSuggestion toSuggestion(Exercise exercise, String query) {
        return new ExerciseSuggestion(
                exercise.getId(),
                exercise.getName(),
                exercise.getSlug(),
                exercise.getCategory(),
                ExerciseNameHighlighter.highlight(exercise.getName(), query));
    }

    private String escapeLike(String query) {
        return query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Set<ExerciseMuscle> resolveMuscles(Exercise exercise, List<ExerciseMuscleCommand> muscles) {
        if (muscles == null || muscles.isEmpty()) {
            return new HashSet<>();
        }

        List<String> slugs =
                muscles.stream().map(ExerciseMuscleCommand::muscleSlug).toList();

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
