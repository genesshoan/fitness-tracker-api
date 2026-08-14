package dev.genesshoan.fitnesstrackerapi.routine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ValidationException;
import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.routine.domain.RoutineExercise;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineExerciseRequestDTO;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineListItemDTO;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineRequestDTO;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineResponseDTO;
import dev.genesshoan.fitnesstrackerapi.routine.mapper.RoutineMapper;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final ExerciseRepository exerciseRepository;
    private final RoutineMapper routineMapper;

    public Page<RoutineListItemDTO> getRoutinesByUserId(User user, Pageable pageable) {
        log.info("Getting routines for user: {} with pageable: {}", user.getId(), pageable);
        return routineRepository.findAllByUserIdAndActiveTrueWithExerciseCount(user.getId(), pageable);
    }

    public RoutineResponseDTO getRoutineById(UUID routineId, User user) {
        log.info("Getting routine by id: {} for user: {}", routineId, user.getId());
        return toResponseDTO(findAndValidateRoutine(routineId, user));
    }

    @Transactional
    public RoutineResponseDTO createRoutine(RoutineRequestDTO dto, User user) {
        log.info("Creating routine with name: {} for user: {}", dto.name(), user.getId());

        if (routineRepository.existsByNameAndUserIdAndActiveTrue(dto.name(), user.getId())) {
            log.warn("Routine with name '{}' already exists for user: {}", dto.name(), user.getId());
            throw new ResourceAlreadyExistsException(
                    "A routine with the name '" + dto.name() + "' already exists for this user");
        }

        var routine = Routine.builder()
                .name(dto.name())
                .description(dto.description())
                .user(user)
                .exercises(new ArrayList<>())
                .build();

        routineRepository.save(routine);
        log.debug("Routine saved with id: {}", routine.getId());

        Map<UUID, Exercise> exercises = resolveAndValidateExercises(dto.exercises());
        log.debug("Validated {} exercises for routine", exercises.size());

        List<RoutineExercise> routineExercises = buildRoutineExercises(dto.exercises(), routine, exercises);

        routine.setExercises(new ArrayList<>(routineExercises));
        log.info("Routine created successfully with id: {} and {} exercises", routine.getId(), routineExercises.size());

        return toResponseDTO(routine);
    }

    @Transactional
    public RoutineResponseDTO updateRoutine(UUID routineId, RoutineRequestDTO dto, User user) {
        log.info("Updating routine: {} for user: {}", routineId, user.getId());

        var routine = findAndValidateRoutine(routineId, user);

        if (!dto.name().equals(routine.getName())
                && routineRepository.existsByNameAndUserIdAndActiveTrue(dto.name(), user.getId())) {
            log.warn("Routine with name '{}' already exists for user: {}", dto.name(), user.getId());
            throw new ResourceAlreadyExistsException(
                    "A routine with the name '" + dto.name() + "' already exists for this user");
        }

        routine.setName(dto.name());
        routine.setDescription(dto.description());
        log.debug("Updated routine metadata: name={}, description={}", dto.name(), dto.description());

        Map<UUID, Exercise> exercisesMap = resolveAndValidateExercises(dto.exercises());

        replaceRoutineExercises(routine, exercisesMap, dto.exercises());
        log.info("Routine updated successfully: {}", routineId);

        return toResponseDTO(routine);
    }

    @Transactional
    public void deleteRoutine(UUID routineId, User user) {
        log.info("Deleting routine: {} for user: {}", routineId, user.getId());
        // TODO: Prevent deletion when an active workout session exists.
        var routine = findAndValidateRoutine(routineId, user);
        routine.setActive(false);
        log.info("Routine deactivated: {}", routineId);
    }

    @Transactional
    public RoutineResponseDTO addRoutineExercise(
            UUID routineId, int position, RoutineExerciseRequestDTO dto, User user) {
        log.info("Adding exercise to routine: {}, position: {}, for user: {}", routineId, position, user.getId());

        var routine = findAndValidateRoutine(routineId, user);

        var exercise = exerciseRepository.findById(dto.exerciseId()).orElseThrow(() -> {
            log.warn("Exercise not found: {}", dto.exerciseId());
            return new ResourceNotFoundException("Exercise not found");
        });

        var maxPosition = routine.getExercises().size();

        var adjustedPosition = Math.max(1, Math.min(position, maxPosition + 1));
        log.debug("Adjusted position from {} to {}", position, adjustedPosition);

        if (!exercise.getCategory().validate(dto.toExerciseMetrics())) {
            log.warn("Invalid data for category {} with exercise: {}", exercise.getCategory(), exercise.getId());
            throw new ValidationException(Map.of(
                    exercise.getId().toString(), List.of("Invalid data for category " + exercise.getCategory())));
        }

        var newRoutineExercise = buildRoutineExercise(routine, exercise, dto, adjustedPosition);

        routine.getExercises().stream()
                .filter(e -> e.getPosition() >= adjustedPosition)
                .forEach(e -> e.setPosition(e.getPosition() + 1));

        routine.getExercises().add(newRoutineExercise);
        log.info("Exercise added to routine: {}, new exercise position: {}", routineId, adjustedPosition);

        return toResponseDTO(routine);
    }

    @Transactional
    public void deleteRoutineExercise(UUID routineId, int position, User user) {
        log.info("Deleting exercise at position: {} from routine: {} for user: {}", position, routineId, user.getId());
        // TODO: Prevent deletion when an active workout session exists.
        var routine = findAndValidateRoutine(routineId, user);

        var routineExercise = routine.getExercises().stream()
                .filter(re -> re.getPosition() == position)
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Routine exercise not found at position: {} for routine: {}", position, routineId);
                    return new ResourceNotFoundException("Routine exercise not found");
                });

        int deletedPosition = routineExercise.getPosition();

        routine.getExercises().remove(routineExercise);
        routineRepository.flush();

        routine.getExercises().stream()
                .filter(e -> e.getPosition() > deletedPosition)
                .forEach(e -> e.setPosition(e.getPosition() - 1));

        log.info("Exercise at position {} removed from routine: {}", deletedPosition, routineId);
    }

    @Transactional
    public RoutineResponseDTO updateRoutineExercise(
            UUID routineId, int position, RoutineExerciseRequestDTO dto, User user) {
        log.info("Updating exercise at position: {} from routine: {} for user: {}", position, routineId, user.getId());

        var routine = findAndValidateRoutine(routineId, user);

        var exercise = exerciseRepository.findById(dto.exerciseId()).orElseThrow(() -> {
            log.warn("Exercise not found: {}", dto.exerciseId());
            return new ResourceNotFoundException("Exercise not found");
        });

        if (!exercise.getCategory().validate(dto.toExerciseMetrics())) {
            log.warn("Invalid data for category {} with exercise: {}", exercise.getCategory(), exercise.getId());
            throw new ValidationException(Map.of(
                    exercise.getId().toString(), List.of("Invalid data for category " + exercise.getCategory())));
        }

        var newRoutineExercise = buildRoutineExercise(routine, exercise, dto, position);

        var routineExercise = routine.getExercises().stream()
                .filter(re -> re.getPosition() == position)
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("No exercise found at position: {} for routine: {}", position, routineId);
                    return new ResourceNotFoundException("No exercise found at position " + position);
                });

        routine.getExercises().remove(routineExercise);

        routine.getExercises().add(newRoutineExercise);
        log.info("Exercise at position {} updated in routine: {}", position, routineId);

        return toResponseDTO(routine);
    }

    private RoutineExercise buildRoutineExercise(
            Routine routine, Exercise exercise, RoutineExerciseRequestDTO dto, int position) {
        log.debug(
                "Building RoutineExercise: routine={}, exercise={}, position={}",
                routine.getId(),
                exercise.getId(),
                position);
        return RoutineExercise.builder()
                .position(position)
                .defaultRestSeconds(dto.defaultRestSeconds())
                .defaultSets(dto.defaultSets())
                .defaultReps(dto.defaultReps())
                .defaultWeightKg(dto.defaultWeightKg())
                .defaultDurationSeconds(dto.defaultDurationSeconds())
                .defaultDistanceKm(dto.defaultDistanceKm())
                .notes(dto.notes())
                .routine(routine)
                .exercise(exercise)
                .build();
    }

    private List<RoutineExercise> buildRoutineExercises(
            List<RoutineExerciseRequestDTO> exerciseDTOs, Routine routine, Map<UUID, Exercise> exercises) {
        log.debug("Building {} routine exercises for routine: {}", exerciseDTOs.size(), routine.getId());
        List<RoutineExercise> routineExercises = new ArrayList<>();
        int position = 1;

        for (RoutineExerciseRequestDTO dto : exerciseDTOs) {
            routineExercises.add(buildRoutineExercise(routine, exercises.get(dto.exerciseId()), dto, position++));
        }

        return routineExercises;
    }

    private Map<UUID, Exercise> resolveAndValidateExercises(List<RoutineExerciseRequestDTO> exerciseDTOs) {
        log.debug("Resolving and validating {} exercises", exerciseDTOs.size());
        Set<UUID> exerciseIds =
                exerciseDTOs.stream().map(RoutineExerciseRequestDTO::exerciseId).collect(Collectors.toSet());

        if (exerciseDTOs.isEmpty()) {
            return Map.of();
        }

        log.debug("Exercise IDs requested: {}", exerciseIds);

        List<Exercise> exercises = exerciseRepository.findAllByIdInAndActiveTrue(exerciseIds);

        if (exercises.size() != exerciseIds.size()) {
            log.warn("One or more exercises not found. Expected: {}, Found: {}", exerciseIds.size(), exercises.size());
            throw new BadRequestException("One or more exercises not found");
        }

        Map<UUID, Exercise> exerciseMap =
                exercises.stream().collect(Collectors.toMap(Exercise::getId, Function.identity()));

        Map<String, List<String>> errors = new HashMap<>();

        for (RoutineExerciseRequestDTO dto : exerciseDTOs) {
            Exercise exercise = exerciseMap.get(dto.exerciseId());
            if (!exercise.getCategory().validate(dto.toExerciseMetrics())) {
                log.warn(
                        "Validation failed for exercise: {} with category: {}",
                        exercise.getId(),
                        exercise.getCategory());
                errors.computeIfAbsent(dto.exerciseId().toString(), k -> new ArrayList<>())
                        .add("Invalid data for category: " + exercise.getCategory());
            }
        }

        if (!errors.isEmpty()) {
            log.error("Validation errors found: {}", errors);
            throw new ValidationException(errors);
        }

        return exerciseMap;
    }

    private Routine findAndValidateRoutine(UUID routineId, User user) {
        log.debug("Finding and validating routine: {} for user: {}", routineId, user.getId());
        Routine routine = routineRepository.findByIdAndActiveTrue(routineId).orElseThrow(() -> {
            log.warn("Routine not found: {}", routineId);
            return new ResourceNotFoundException("Routine not found");
        });

        if (!routine.getUser().getId().equals(user.getId())) {
            log.warn("User: {} does not have permission to access routine: {}", user.getId(), routineId);
            throw new ResourceNotFoundException("Routine not found");
        }

        return routine;
    }

    private RoutineResponseDTO toResponseDTO(Routine routine) {
        if (routine.getExercises() != null) {
            routine.getExercises().sort(Comparator.comparingInt(RoutineExercise::getPosition));
        }
        return routineMapper.toRoutineResponseDTO(routine);
    }

    private void replaceRoutineExercises(
            Routine routine, Map<UUID, Exercise> exercisesMap, List<RoutineExerciseRequestDTO> exerciseDTOs) {
        log.debug("Replacing exercises for routine: {}", routine.getId());
        routine.getExercises().clear();

        List<RoutineExercise> newExercises = buildRoutineExercises(exerciseDTOs, routine, exercisesMap);

        routine.getExercises().addAll(newExercises);
        log.debug("Exercises replaced successfully for routine: {}", routine.getId());
    }
}
