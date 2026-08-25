package dev.genesshoan.fitnesstrackerapi.workout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ValidationException;
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

    public Page<WorkoutSessionListItemDTO> getAllWorkoutSessions(UUID userId, Pageable pageable) {
        log.info("Getting workouts sessions from user: {} with pageable: {}", userId, pageable);
        return workoutSessionRepository
                .findAllByUserId(userId, pageable)
                .map(workoutSessionMapper::toWorkoutSessionListItemDTO);
    }

    public WorkoutSessionResponseDTO getWorkoutSessionById(UUID sessionId, UUID userId) {
        log.info("Getting workout session: {} for user: {}", sessionId, userId);
        return workoutSessionMapper.toWorkoutSessionResponseDTO(getOrThrow(
                workoutSessionRepository.findWithExercisesAndSetsByIdAndUserId(sessionId, userId),
                "Workout session",
                sessionId));
    }

    @Transactional
    public WorkoutSessionResponseDTO createWorkoutSessionFromRoutine(UUID routineId, UUID userId) {
        Routine routine = routineRepository
                .findByIdAndUserIdAndActiveTrue(routineId, userId)
                .orElseThrow(() -> {
                    log.warn("Routine: {} not found", routineId);
                    return new ResourceNotFoundException("Routine not found");
                });

        WorkoutSession session = WorkoutSession.builder()
                .user(userRepository.getReferenceById(userId))
                .routine(routine)
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

        log.info("Created workout session: {} from routine: {} from user: {}", session.getId(), routineId, userId);

        return workoutSessionMapper.toWorkoutSessionResponseDTO(workoutSessionRepository.save(session));
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
                .completedAt(dto.completedAt())
                .user(userRepository.getReferenceById(userId))
                .build();

        buildSessionExercises(dto.exercises(), workoutSession, exercises);

        log.info("Created workout session: {} for user: {}", workoutSession.getId(), userId);

        return workoutSessionMapper.toWorkoutSessionResponseDTO(workoutSessionRepository.save(workoutSession));
    }

    @Transactional
    public void updateWorkoutSessionNotes(UUID sessionId, UUID userId, NotesUpdateRequestDTO dto) {
        WorkoutSession session =
                getOrThrow(workoutSessionRepository.findByIdAndUserId(sessionId, userId), "Workout session", sessionId);

        if (session.getStatus() == SessionStatus.COMPLETED) {
            log.warn("Invalid workout session notes update attempt: The session {} is already finished", sessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        session.setNotes(dto.notes());
    }

    @Transactional
    public void completeWorkoutSession(UUID sessionId, UUID userId) {
        log.debug("Completing session workout: {} from user: {}", sessionId, userId);
        WorkoutSession session =
                getOrThrow(workoutSessionRepository.findByIdAndUserId(sessionId, userId), "Workout session", sessionId);

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
        WorkoutSession session =
                getOrThrow(workoutSessionRepository.findByIdAndUserId(sessionId, userId), "Workout session", sessionId);

        workoutSessionRepository.delete(session);
        log.info("Deleted session workout: {} from user: {}", sessionId, userId);
    }

    @Transactional
    public SessionExerciseAddedResponseDTO addNewSessionExercise(
            UUID sessionId, UUID userId, SessionExerciseRequestDTO dto) {
        log.debug("Adding new session exercise into position: {} to session: {}", dto.position(), sessionId);
        WorkoutSession session = getOrThrow(
                workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId),
                "Workout session",
                sessionId);

        if (session.getStatus() == SessionStatus.COMPLETED) {
            log.warn("Invalid session exercise add attempt: The session {} is already finished", sessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        Map<String, List<String>> errors = new HashMap<>();

        Exercise exercise = exerciseRepository
                .findByIdAndActiveTrue(dto.exerciseId())
                .orElseThrow(() -> {
                    log.warn("Exercise {} not found", dto.exerciseId());
                    return new ResourceNotFoundException("Exercise not found");
                });

        validateSetsData(dto.sets(), exercise, errors);

        if (!errors.isEmpty()) {
            log.warn("Validation errors found: {}", errors);
            throw new ValidationException(errors);
        }

        SessionExercise sessionExercise = buildSessionExercise(dto, exercise);

        session.addExerciseAt(sessionExercise, dto.position());

        List<SessionExercise> shifted = session.getExercises().stream()
                .filter(e -> !e.equals(sessionExercise))
                .sorted(Comparator.comparingInt(SessionExercise::getPosition))
                .toList();

        log.info(
                "Session exercise {} added into position: {} to session: {}",
                sessionExercise.getId(),
                sessionExercise.getPosition(),
                session);

        return sessionExerciseMapper.toSessionExerciseAddedResponseDTO(sessionExercise, shifted);
    }

    @Transactional
    public void updateSessionExerciseNotes(
            UUID workoutSessionId, UUID sessionExerciseId, UUID userId, NotesUpdateRequestDTO dto) {
        SessionExercise sessionExercise = getOrThrow(
                sessionExerciseRepository.findWithWorkoutSessionByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
                        sessionExerciseId, workoutSessionId, userId),
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
        WorkoutSession workoutSession = getOrThrow(
                workoutSessionRepository.findWithExercisesByIdAndUserId(workoutSessionId, userId),
                "Workout session",
                workoutSessionId);

        SessionExercise sessionExercise = workoutSession.getExercises().stream()
                .filter(se -> se.getId().equals(sessionExerciseId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Session exercise not found: {}", sessionExerciseId);
                    return new ResourceNotFoundException("Session exercise not found");
                });

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
        WorkoutSession workoutSession = getOrThrow(
                workoutSessionRepository.findWithExercisesByIdAndUserId(workoutSessionId, userId),
                "Workout session",
                workoutSessionId);

        SessionExercise sessionExercise = workoutSession.getExercises().stream()
                .filter(se -> se.getId().equals(sessionExerciseId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Session exercise not found: {}", sessionExerciseId);
                    return new ResourceNotFoundException("Session exercise not found");
                });

        if (workoutSession.getStatus() == SessionStatus.COMPLETED) {
            log.warn(
                    "Invalid session exercise {} delete attempt: The session {} is already finished",
                    sessionExerciseId,
                    workoutSessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        workoutSession.removeExercise(sessionExercise);

        log.info("Session exercise: {} removed sucessfully from session: {}", sessionExerciseId, workoutSessionId);

        return sessionExerciseMapper.toSessionExercisePositionDTOList(workoutSession.getExercises());
    }

    @Transactional
    public SessionSetResponseDTO addNewSessionSet(
            UUID workoutSessionId, UUID sessionExerciseId, UUID userId, SessionSetRequestDTO dto) {
        log.debug("Adding new session set to session exercise: {}", sessionExerciseId);
        SessionExercise sessionExercise = getOrThrow(
                sessionExerciseRepository
                        .findWithWorkoutSessionAndExerciseByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
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

        if (!sessionExercise.getExercise().getCategory().validate(dto.toExerciseMetrics())) {
            throw new BadRequestException("Invalid data for exercise category: "
                    + sessionExercise.getExercise().getCategory());
        }

        SessionSet sessionSet = buildSessionSet(dto, sessionExercise.getSets().size() + 1);

        sessionExercise.addSet(sessionSet);

        return sessionSetMapper.toSessionSetResponseDTO(sessionSetRepository.save(sessionSet));
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

        SessionSet sessionSet = getOrThrow(
                sessionSetRepository
                        .findByIdAndSessionExerciseIdAndSessionExerciseWorkoutSessionIdAndSessionExerciseWorkoutSessionUserId(
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

        SessionExercise sessionExercise = getOrThrow(
                sessionExerciseRepository.findWithWorkoutSessionByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
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

        SessionSet sessionSet = getOrThrow(
                sessionExercise.getSets().stream()
                        .filter(s -> s.getId().equals(sessionSetId))
                        .findFirst(),
                "Session set",
                sessionSetId);

        if (sessionExercise.getSets().size() > 1) {
            sessionExercise.removeSet(sessionSet);
        } else {
            sessionExerciseRepository.delete(sessionExercise);
        }
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
            Map<UUID, Exercise> exercises) {

        for (int i = 0; i < exerciseDTOs.size(); i++) {
            SessionExerciseRequestDTO dto = exerciseDTOs.get(i);
            Exercise exercise = exercises.get(dto.exerciseId());

            SessionExercise sessionExercise = buildSessionExercise(dto, exercise);

            workoutSession.addExerciseAt(sessionExercise, i + 1);
        }
    }

    private SessionExercise buildSessionExercise(SessionExerciseRequestDTO dto, Exercise exercise) {

        SessionExercise sessionExercise =
                SessionExercise.builder().notes(dto.notes()).exercise(exercise).build();

        for (int i = 0; i < dto.sets().size(); i++) {
            sessionExercise.addSet(buildSessionSet(dto.sets().get(i), i + 1));
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

    private <T> T getOrThrow(Optional<T> result, String resourceName, UUID id) {
        return result.orElseThrow(() -> {
            log.warn("{} not found: {}", resourceName, id);
            return new ResourceNotFoundException(resourceName + " not found");
        });
    }
}
