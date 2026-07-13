package dev.genesshoan.fitnesstrackerapi.routine;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.UnauthorizedException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final ExerciseRepository exerciseRepository;
    private final RoutineMapper routineMapper;

    public Page<RoutineListItemDTO> getRoutinesByUserId(User user, Pageable pageable) {
        return routineRepository.findAllByUserIdAndActiveTrueWithExerciseCount(user.getId(), pageable);
    }

    public RoutineResponseDTO getRoutineById(UUID routineId, User user) {
        return routineMapper.toRoutineResponseDTO(findAndValidateRoutine(routineId, user));
    }

    @Transactional
    public RoutineResponseDTO createRoutine(RoutineRequestDTO dto, User user) {
        if (routineRepository.existsByNameAndUserIdAndActiveTrue(dto.name(), user.getId())) {
            throw new ResourceAlreadyExistsException(
                    "A routine with the name '" + dto.name() + "' already exists for this user");
        }

        var routine = Routine.builder()
                .name(dto.name())
                .description(dto.description())
                .user(user)
                .exercises(new HashSet<>())
                .build();

        routineRepository.save(routine);

        Map<UUID, Exercise> exercises = resolveAndValidateExercises(dto.exercises());

        List<RoutineExercise> routineExercises = buildRoutineExercises(dto.exercises(), routine, exercises);

        routine.setExercises(new HashSet<>(routineExercises));

        return routineMapper.toRoutineResponseDTO(routine);
    }

    @Transactional
    public RoutineResponseDTO updateRoutine(UUID routineId, RoutineRequestDTO dto, User user) {
        var routine = findAndValidateRoutine(routineId, user);

        if (!dto.name().equals(routine.getName())
                && routineRepository.existsByNameAndUserIdAndActiveTrue(dto.name(), user.getId())) {
            throw new ResourceAlreadyExistsException(
                    "A routine with the name '" + dto.name() + "' already exists for this user");
        }

        routine.setName(dto.name());
        routine.setDescription(dto.description());

        Map<UUID, Exercise> exercisesMap = resolveAndValidateExercises(dto.exercises());

        replaceRoutineExercises(routine, exercisesMap, dto.exercises());

        return routineMapper.toRoutineResponseDTO(routine);
    }

    @Transactional
    public void deleteRoutine(UUID routineId, User user) {
        var routine = findAndValidateRoutine(routineId, user);
        routine.setActive(false);
    }

    @Transactional
    public RoutineResponseDTO addRoutineExercise(
            UUID routineId, int position, RoutineExerciseRequestDTO dto, User user) {
        var routine = findAndValidateRoutine(routineId, user);

        var exercise = exerciseRepository
                .findById(dto.exerciseId())
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found"));

        var maxPosition = routine.getExercises().size();

        var adjustedPosition = Math.max(1, Math.min(position, maxPosition + 1));

        if (!exercise.getCategory().validate(dto)) {
            throw new ValidationException(Map.of(
                    exercise.getId().toString(), List.of("Invalid data for category " + exercise.getCategory())));
        }

        var newRoutineExercise = buildRoutineExercise(routine, exercise, dto, adjustedPosition);

        routine.getExercises().stream()
                .filter(e -> e.getPosition() >= adjustedPosition)
                .forEach(e -> e.setPosition(e.getPosition() + 1));

        routine.getExercises().add(newRoutineExercise);

        return routineMapper.toRoutineResponseDTO(routine);
    }

    @Transactional
    public void deleteRoutineExercise(UUID routineId, int position, User user) {
        var routine = findAndValidateRoutine(routineId, user);

        var routineExercise = routine.getExercises().stream()
                .filter(re -> re.getPosition() == position)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Routine exercise not found"));

        int deletedPosition = routineExercise.getPosition();

        routine.getExercises().remove(routineExercise);

        routine.getExercises().stream()
                .filter(e -> e.getPosition() > deletedPosition)
                .forEach(e -> e.setPosition(e.getPosition() - 1));
    }

    @Transactional
    public RoutineResponseDTO updateRoutineExercise(
            UUID routineId, int position, RoutineExerciseRequestDTO dto, User user) {
        var routine = findAndValidateRoutine(routineId, user);

        var exercise = exerciseRepository
                .findById(dto.exerciseId())
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found"));

        if (!exercise.getCategory().validate(dto)) {
            throw new ValidationException(Map.of(
                    exercise.getId().toString(), List.of("Invalid data for category " + exercise.getCategory())));
        }

        var newRoutineExercise = buildRoutineExercise(routine, exercise, dto, position);

        var routineExercise = routine.getExercises().stream()
                .filter(re -> re.getPosition() == position)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No exercise found at position " + position));

        routine.getExercises().remove(routineExercise);

        routine.getExercises().add(newRoutineExercise);

        return routineMapper.toRoutineResponseDTO(routine);
    }

    private RoutineExercise buildRoutineExercise(
            Routine routine, Exercise exercise, RoutineExerciseRequestDTO dto, int position) {
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
        List<RoutineExercise> routineExercises = new ArrayList<>();
        int position = 1;

        for (RoutineExerciseRequestDTO dto : exerciseDTOs) {
            routineExercises.add(buildRoutineExercise(routine, exercises.get(dto.exerciseId()), dto, position++));
        }

        return routineExercises;
    }

    private Map<UUID, Exercise> resolveAndValidateExercises(List<RoutineExerciseRequestDTO> exerciseDTOs) {
        Set<UUID> exerciseIds =
                exerciseDTOs.stream().map(RoutineExerciseRequestDTO::exerciseId).collect(Collectors.toSet());

        List<Exercise> exercises = exerciseRepository.findAllById(exerciseIds);

        if (exercises.size() != exerciseIds.size()) {
            throw new BadRequestException("One or more exercises not found");
        }

        Map<UUID, Exercise> exerciseMap =
                exercises.stream().collect(Collectors.toMap(Exercise::getId, Function.identity()));

        Map<String, List<String>> errors = new HashMap<>();

        for (RoutineExerciseRequestDTO dto : exerciseDTOs) {
            Exercise exercise = exerciseMap.get(dto.exerciseId());
            if (!exercise.getCategory().validate(dto)) {
                errors.computeIfAbsent(dto.exerciseId().toString(), k -> new ArrayList<>())
                        .add("Invalid data for category: " + exercise.getCategory());
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        return exerciseMap;
    }

    private Routine findAndValidateRoutine(UUID routineId, User user) {
        Routine routine = routineRepository
                .findByIdAndActiveTrue(routineId)
                .orElseThrow(() -> new ResourceNotFoundException("Routine not found"));

        if (!routine.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not have permission to access this routine");
        }

        return routine;
    }

    private void replaceRoutineExercises(
            Routine routine, Map<UUID, Exercise> exercisesMap, List<RoutineExerciseRequestDTO> exerciseDTOs) {
        routine.getExercises().clear();

        List<RoutineExercise> newExercises = buildRoutineExercises(exerciseDTOs, routine, exercisesMap);

        routine.getExercises().addAll(newExercises);
    }
}
