package dev.genesshoan.fitnesstrackerapi.workout;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ValidationException;
import dev.genesshoan.fitnesstrackerapi.common.mapper.ExerciseMetricsMapper;
import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ExerciseFinder;
import dev.genesshoan.fitnesstrackerapi.routine.RoutineRepository;
import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.user.UserRepository;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionStatus;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import dev.genesshoan.fitnesstrackerapi.workout.dto.NotesUpdateRequestDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.PositionRequestDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExerciseAddedResponseDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExercisePositionDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExerciseRequestDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionSetRequestDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionSetResponseDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionListItemDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionRequestDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionResponseDTO;
import dev.genesshoan.fitnesstrackerapi.workout.mapper.SessionExerciseMapper;
import dev.genesshoan.fitnesstrackerapi.workout.mapper.SessionSetMapper;
import dev.genesshoan.fitnesstrackerapi.workout.mapper.WorkoutSessionMapper;
import dev.genesshoan.fitnesstrackerapi.workout.repository.SessionExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.workout.repository.SessionSetRepository;
import dev.genesshoan.fitnesstrackerapi.workout.repository.WorkoutSessionRepository;
import dev.genesshoan.fitnesstrackerapi.workout.repository.projection.LastSetProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkoutSessionService {

    private final WorkoutSessionRepository workoutSessionRepository;
    private final SessionExerciseRepository sessionExerciseRepository;
    private final SessionSetRepository sessionSetRepository;
    private final RoutineRepository routineRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;

    private final ExerciseFinder exerciseFinder;

    private final WorkoutSessionMapper workoutSessionMapper;
    private final SessionExerciseMapper sessionExerciseMapper;
    private final SessionSetMapper sessionSetMapper;
    private final ExerciseMetricsMapper exerciseMetricsMapper;

    public Page<WorkoutSessionListItemDTO> getAllWorkoutSessions(UUID userId, Pageable pageable) {

        log.info("Getting workouts sessions from user: {} with pageable: {}", userId, pageable);

        return workoutSessionRepository
                .findAllByUserId(userId, pageable)
                .map(workoutSessionMapper::toWorkoutSessionListItemDTO);
    }

    public WorkoutSessionResponseDTO getWorkoutSessionById(UUID sessionId, UUID userId) {

        log.info("Getting workout session: {} for user: {}", sessionId, userId);

        return workoutSessionMapper.toWorkoutSessionResponseDTO(getOrThrowResourceNotFound(
                workoutSessionRepository.findWithExercisesAndSetsByIdAndUserId(sessionId, userId),
                "Workout session",
                sessionId));
    }

    @Transactional
    public WorkoutSessionResponseDTO createWorkoutSessionFromRoutine(UUID routineId, UUID userId) {

        Routine routine = getOrThrowResourceNotFound(
                routineRepository.findByIdAndUserIdAndActiveTrue(routineId, userId), "Routine", routineId);

        WorkoutSession session = WorkoutSession.builder()
                .user(userRepository.getReferenceById(userId))
                .routine(routine)
                .startedAt(Instant.now())
                .build();

        routine.getExercises().forEach(re -> {
            SessionExercise exercise = new SessionExercise();

            session.addExerciseAt(exercise, re.getPosition());

            for (int i = 1; i <= re.getDefaultSets(); i++) {
                SessionSet set = SessionSet.builder()
                        .setNumber(i)
                        .reps(re.getDefaultReps())
                        .weightKg(re.getDefaultWeightKg())
                        .durationSeconds(re.getDefaultDurationSeconds())
                        .distanceKm(re.getDefaultDistanceKm())
                        .build();

                exercise.addSet(set);
            }
        });

        WorkoutSession saved = workoutSessionRepository.save(session);

        log.info("Created workout session: {} from routine: {} from user: {}", session.getId(), routineId, userId);

        return workoutSessionMapper.toWorkoutSessionResponseDTO(saved);
    }

    @Transactional
    public WorkoutSessionResponseDTO createWorkoutSessionFromScratch(WorkoutSessionRequestDTO dto, UUID userId) {

        validateSessionRequest(dto);

        Map<String, List<String>> errors = new HashMap<>();

        Map<UUID, Exercise> exercises = exerciseFinder.findActiveByIds(
                dto.exercises().stream()
                        .map(SessionExerciseRequestDTO::exerciseId)
                        .toList(),
                errors);

        validateSetsData(dto.exercises(), exercises, errors);

        if (!errors.isEmpty()) {
            log.warn("Validation errors found: {}", errors);
            throw new ValidationException(errors);
        }

        WorkoutSession workoutSession = WorkoutSession.builder()
                .status(dto.status())
                .notes(dto.notes())
                .startedAt(dto.startedAt())
                .completedAt(dto.completedAt())
                .user(userRepository.getReferenceById(userId))
                .build();

        Map<UUID, LastSetProjection> defaultsByExercise = getDefaultsByExercise(dto.exercises(), userId);

        buildSessionExercises(dto.exercises(), workoutSession, exercises, defaultsByExercise, userId);

        WorkoutSession saved = workoutSessionRepository.save(workoutSession);

        log.info("Created workout session: {} for user: {}", workoutSession.getId(), userId);

        return workoutSessionMapper.toWorkoutSessionResponseDTO(saved);
    }

    @Transactional
    public void updateWorkoutSessionNotes(UUID sessionId, UUID userId, NotesUpdateRequestDTO dto) {

        WorkoutSession session = getOrThrowResourceNotFound(
                workoutSessionRepository.findByIdAndUserId(sessionId, userId), "Workout session", sessionId);

        if (session.getStatus() == SessionStatus.COMPLETED) {
            log.warn("Invalid workout session notes update attempt: The session {} is already finished", sessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        session.setNotes(dto.notes());
    }

    @Transactional
    public void completeWorkoutSession(UUID sessionId, UUID userId) {

        log.debug("Completing session workout: {} from user: {}", sessionId, userId);

        WorkoutSession session = getOrThrowResourceNotFound(
                workoutSessionRepository.findByIdAndUserId(sessionId, userId), "Workout session", sessionId);

        if (session.getStatus() == SessionStatus.COMPLETED) {
            log.warn("Invalid workout session finish attempt: The session {} is already finished", sessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        session.finish();

        log.info("Completed session workout: {} from user: {}", sessionId, userId);
    }

    @Transactional
    public void deleteWorkoutSession(UUID sessionId, UUID userId) {

        log.debug("Deleting session workout: {} from user: {}", sessionId, userId);

        WorkoutSession session = getOrThrowResourceNotFound(
                workoutSessionRepository.findByIdAndUserId(sessionId, userId), "Workout session", sessionId);

        workoutSessionRepository.delete(session);

        log.info("Deleted session workout: {} from user: {}", sessionId, userId);
    }

    @Transactional
    public SessionExerciseAddedResponseDTO addNewSessionExercise(
            UUID sessionId, UUID userId, SessionExerciseRequestDTO dto) {

        log.debug("Adding new session exercise into position: {} to session: {}", dto.position(), sessionId);

        WorkoutSession session = getOrThrowResourceNotFound(
                workoutSessionRepository.findForUpdateWithExercises(sessionId, userId), "Workout session", sessionId);

        if (session.getStatus() == SessionStatus.COMPLETED) {
            log.warn("Invalid session exercise add attempt: The session {} is already finished", sessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        Map<String, List<String>> errors = new HashMap<>();

        Exercise exercise = getOrThrowResourceNotFound(
                exerciseRepository.findByIdAndActiveTrue(dto.exerciseId()), "Exercise", dto.exerciseId());

        validateSetsData(dto.sets(), exercise, errors);

        if (!errors.isEmpty()) {
            log.warn("Validation errors found: {}", errors);
            throw new ValidationException(errors);
        }

        Map<UUID, LastSetProjection> defaultByExercise = getDefaultsByExercise(List.of(dto), userId);

        SessionExercise sessionExercise = buildSessionExercise(dto, exercise, defaultByExercise, userId);

        session.addExerciseAt(sessionExercise, dto.position());

        List<SessionExercise> shifted = session.getExercises().stream()
                .filter(e -> !e.equals(sessionExercise))
                .sorted(Comparator.comparingInt(SessionExercise::getPosition))
                .toList();

        log.info(
                "Session exercise {} added into position: {} to session: {}",
                sessionExercise.getId(),
                sessionExercise.getPosition(),
                session.getId());

        return sessionExerciseMapper.toSessionExerciseAddedResponseDTO(sessionExercise, shifted);
    }

    @Transactional
    public void updateSessionExerciseNotes(
            UUID workoutSessionId, UUID sessionExerciseId, UUID userId, NotesUpdateRequestDTO dto) {

        SessionExercise sessionExercise = getOrThrowResourceNotFound(
                sessionExerciseRepository.findWithWorkoutSession(sessionExerciseId, workoutSessionId, userId),
                "Session exercise",
                sessionExerciseId);

        if (sessionExercise.getWorkoutSession().getStatus() == SessionStatus.COMPLETED) {
            log.warn(
                    "Invalid session exercise notes update attempt: The session {} is already finished",
                    workoutSessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        sessionExercise.setNotes(dto.notes());
    }

    @Transactional
    public List<SessionExercisePositionDTO> updateSessionExercisePosition(
            UUID workoutSessionId, UUID sessionExerciseId, UUID userId, PositionRequestDTO dto) {

        log.debug("Updating session exercise: {} position from session: {}", sessionExerciseId, workoutSessionId);

        WorkoutSession workoutSession = getOrThrowResourceNotFound(
                workoutSessionRepository.findForUpdateWithExercises(workoutSessionId, userId),
                "Workout session",
                workoutSessionId);

        SessionExercise sessionExercise = getOrThrowResourceNotFound(
                workoutSession.findExercise(sessionExerciseId), "Session exercise", sessionExerciseId);

        if (workoutSession.getStatus() == SessionStatus.COMPLETED) {
            log.warn(
                    "Invalid session exercise {} position update attempt: The session {} is already finished",
                    sessionExerciseId,
                    workoutSessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        workoutSession.moveExercise(sessionExercise, dto.position());

        workoutSession.getExercises().sort(Comparator.comparingInt(SessionExercise::getPosition));

        log.info(
                "Session exercise: {} position updated sucessfully from session: {}",
                sessionExerciseId,
                workoutSessionId);

        return sessionExerciseMapper.toSessionExercisePositionDTOList(workoutSession.getExercises());
    }

    @Transactional
    public List<SessionExercisePositionDTO> deleteSessionExercise(
            UUID workoutSessionId, UUID sessionExerciseId, UUID userId) {

        log.debug("Removing session exercise: {} from session: {}", sessionExerciseId, workoutSessionId);

        WorkoutSession workoutSession = getOrThrowResourceNotFound(
                workoutSessionRepository.findForUpdateWithExercises(workoutSessionId, userId),
                "Workout session",
                workoutSessionId);

        SessionExercise sessionExercise = getOrThrowResourceNotFound(
                workoutSession.findExercise(sessionExerciseId), "Session exercise", sessionExerciseId);

        if (workoutSession.getStatus() == SessionStatus.COMPLETED) {
            log.warn(
                    "Invalid session exercise {} delete attempt: The session {} is already finished",
                    sessionExerciseId,
                    workoutSessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        workoutSession.removeExercise(sessionExercise);

        log.info("Session exercise: {} removed successfully from session: {}", sessionExerciseId, workoutSessionId);

        return sessionExerciseMapper.toSessionExercisePositionDTOList(workoutSession.getExercises());
    }

    @Transactional
    public SessionSetResponseDTO addNewSessionSet(
            UUID workoutSessionId, UUID sessionExerciseId, UUID userId, SessionSetRequestDTO dto) {

        log.debug("Adding new session set to session exercise: {}", sessionExerciseId);

        SessionExercise sessionExercise = getOrThrowResourceNotFound(
                sessionExerciseRepository.findForUpdateWithWorkoutSessionAndExerciseAndSets(
                        sessionExerciseId, workoutSessionId, userId),
                "Session exercise",
                sessionExerciseId);

        if (sessionExercise.getWorkoutSession().getStatus() == SessionStatus.COMPLETED) {
            log.warn(
                    "Invalid session set {} add attempt: The session {} is already finished",
                    sessionExerciseId,
                    workoutSessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        SessionSetRequestDTO effectiveDto =
                dto.isEmpty() ? resolveDefaultsSetForExistingExercise(sessionExercise, userId) : dto;

        if (!dto.isEmpty() && !sessionExercise.getExercise().getCategory().validate(dto.toExerciseMetrics())) {
            throw new BadRequestException("Invalid data for exercise category: "
                    + sessionExercise.getExercise().getCategory());
        }

        SessionSet sessionSet = buildSessionSet(effectiveDto, sessionExercise.getSetCount() + 1);

        sessionExercise.addSet(sessionSet);

        SessionSet saved = sessionSetRepository.save(sessionSet);

        return sessionSetMapper.toSessionSetResponseDTO(saved);
    }

    @Transactional
    public SessionSetResponseDTO updateSessionSet(
            UUID workoutSessionId, UUID sessionExerciseId, UUID sessionSetId, UUID userId, SessionSetRequestDTO dto) {

        log.debug(
                "Updating session set: {} from exercise: {}, session: {} and user:{}",
                sessionSetId,
                sessionExerciseId,
                workoutSessionId,
                userId);

        SessionSet sessionSet = getOrThrowResourceNotFound(
                sessionSetRepository.findWithSessionExerciseAndWorkoutSessionAndExercise(
                        sessionSetId, sessionExerciseId, workoutSessionId, userId),
                "Session set",
                sessionSetId);

        if (sessionSet.getSessionExercise().getWorkoutSession().getStatus() == SessionStatus.COMPLETED) {
            log.warn(
                    "Invalid session set {} update attempt: The session {} is already finished",
                    sessionSetId,
                    workoutSessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        if (!sessionSet.getSessionExercise().getExercise().getCategory().validate(dto.toExerciseMetrics())) {
            throw new BadRequestException("Invalid data for exercise category: "
                    + sessionSet.getSessionExercise().getExercise().getCategory());
        }

        applySessionSetUpdate(sessionSet, dto);

        return sessionSetMapper.toSessionSetResponseDTO(sessionSet);
    }

    @Transactional
    public void deleteSessionSet(UUID workoutSessionId, UUID sessionExerciseId, UUID sessionSetId, UUID userId) {

        log.debug(
                "Deleting session set from exercise: {}, session: {} and user:{}",
                sessionExerciseId,
                workoutSessionId,
                userId);

        SessionExercise sessionExercise = getOrThrowResourceNotFound(
                sessionExerciseRepository.findForUpdateWithWorkoutSessionAndSets(
                        sessionExerciseId, workoutSessionId, userId),
                "Session exercise",
                sessionExerciseId);

        if (sessionExercise.getWorkoutSession().getStatus() == SessionStatus.COMPLETED) {
            log.warn(
                    "Invalid session set {} delete attempt: The session {} is already finished",
                    sessionSetId,
                    workoutSessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        SessionSet sessionSet =
                getOrThrowResourceNotFound(sessionExercise.findSet(sessionSetId), "Session set", sessionSetId);

        if (sessionExercise.getSetCount() > 1) {
            sessionExercise.removeSet(sessionSet);
        } else {
            sessionExerciseRepository.delete(sessionExercise);
        }
    }

    private SessionSetRequestDTO resolveDefaultsSetForExistingExercise(SessionExercise sessionExercise, UUID userId) {
        return sessionExercise.getSets().stream()
                .max(Comparator.comparingInt(SessionSet::getSetNumber))
                .map(ss -> sessionSetMapper.toSessionSetRequestDTO(ss))
                .orElseGet(() -> exerciseMetricsMapper.toDefaultSessionSetRequestDTO(
                        sessionExercise.getExercise().getCategory().defaultMetrics()));
    }

    private SessionSetRequestDTO resolveDefaultSetForNewExercise(
            Exercise exercise, Map<UUID, LastSetProjection> defaultSets) {

        return Optional.ofNullable(defaultSets.get(exercise.getId()))
                .map(lsp -> new SessionSetRequestDTO(
                        1, lsp.getReps(), lsp.getWeightKg(), lsp.getDurationSeconds(), lsp.getDistanceKm(), false))
                .orElseGet(() -> exerciseMetricsMapper.toDefaultSessionSetRequestDTO(
                        exercise.getCategory().defaultMetrics()));
    }

    private void applySessionSetUpdate(SessionSet sessionSet, SessionSetRequestDTO dto) {

        sessionSet.setReps(dto.reps());
        sessionSet.setWeightKg(dto.weightKg());
        sessionSet.setDurationSeconds(dto.durationSeconds());
        sessionSet.setDistanceKm(dto.distanceKm());
        sessionSet.setCompleted(dto.completed());
    }

    private void validateSetsData(
            List<SessionExerciseRequestDTO> exerciseDTOs,
            Map<UUID, Exercise> exercises,
            Map<String, List<String>> errors) {

        for (SessionExerciseRequestDTO exerciseDTO : exerciseDTOs) {
            Exercise exercise = exercises.get(exerciseDTO.exerciseId());

            if (exercise != null) {
                validateSetsData(exerciseDTO.sets(), exercise, errors);
            }
        }
    }

    private void validateSetsData(
            List<SessionSetRequestDTO> setDTOs, Exercise exercise, Map<String, List<String>> errors) {

        for (int i = 0; i < setDTOs.size(); i++) {
            SessionSetRequestDTO setDTO = setDTOs.get(i);

            if (!exercise.getCategory().validate(setDTO.toExerciseMetrics())) {
                errors.computeIfAbsent(exercise.getId().toString(), k -> new ArrayList<>())
                        .add("Invalid data for set " + (i + 1) + " for category: " + exercise.getCategory());
            }
        }
    }

    private void buildSessionExercises(
            List<SessionExerciseRequestDTO> exerciseDTOs,
            WorkoutSession workoutSession,
            Map<UUID, Exercise> exercises,
            Map<UUID, LastSetProjection> defaultSets,
            UUID userId) {

        for (int i = 0; i < exerciseDTOs.size(); i++) {
            SessionExerciseRequestDTO dto = exerciseDTOs.get(i);
            Exercise exercise = exercises.get(dto.exerciseId());

            SessionExercise sessionExercise = buildSessionExercise(dto, exercise, defaultSets, userId);

            workoutSession.addExerciseAt(sessionExercise, i + 1);
        }
    }

    private SessionExercise buildSessionExercise(
            SessionExerciseRequestDTO dto, Exercise exercise, Map<UUID, LastSetProjection> defaultSets, UUID userId) {

        SessionExercise sessionExercise =
                SessionExercise.builder().notes(dto.notes()).exercise(exercise).build();

        List<SessionSetRequestDTO> sets =
                dto.sets().isEmpty() ? List.of(resolveDefaultSetForNewExercise(exercise, defaultSets)) : dto.sets();

        for (int i = 0; i < sets.size(); i++) {
            sessionExercise.addSet(buildSessionSet(sets.get(i), i + 1));
        }

        return sessionExercise;
    }

    private SessionSet buildSessionSet(SessionSetRequestDTO dto, int setNumber) {

        return SessionSet.builder()
                .setNumber(setNumber)
                .reps(dto.reps())
                .weightKg(dto.weightKg())
                .durationSeconds(dto.durationSeconds())
                .distanceKm(dto.distanceKm())
                .completed(dto.completed())
                .build();
    }

    private void validateSessionRequest(WorkoutSessionRequestDTO dto) {

        if (dto.completedAt() != null && dto.completedAt().isAfter(Instant.now())) {
            throw new BadRequestException("completedAt cannot be in the future");
        }

        if (dto.status() == SessionStatus.COMPLETED && dto.completedAt() == null) {
            throw new BadRequestException("The completedAt field cannot be null for a completed workout session");
        }

        if (dto.completedAt() != null && dto.status() != SessionStatus.COMPLETED) {
            throw new BadRequestException("The completedAt field can only be provided for a completed workout session");
        }

        if (dto.status() == SessionStatus.COMPLETED && dto.exercises().isEmpty()) {
            throw new BadRequestException("A completed workout session must have at least one exercise");
        }
    }

    private <T> T getOrThrowResourceNotFound(Optional<T> result, String resourceName, UUID id) {

        return result.orElseThrow(() -> {
            log.warn("{} not found: {}", resourceName, id);
            return new ResourceNotFoundException(resourceName + " not found");
        });
    }

    private Map<UUID, LastSetProjection> getDefaultsByExercise(List<SessionExerciseRequestDTO> dtos, UUID userId) {
        Set<UUID> exerciseIdsWithoutSets = dtos.stream()
                .filter(eDto -> eDto.sets().isEmpty())
                .map(SessionExerciseRequestDTO::exerciseId)
                .collect(Collectors.toSet());

        Map<UUID, LastSetProjection> defaultsByExercise =
                sessionSetRepository.findLastSetsByExerciseIdAndUserId(exerciseIdsWithoutSets, userId).stream()
                        .collect(Collectors.toMap(lsp -> lsp.getExerciseId(), Function.identity()));
        return defaultsByExercise;
    }
}
