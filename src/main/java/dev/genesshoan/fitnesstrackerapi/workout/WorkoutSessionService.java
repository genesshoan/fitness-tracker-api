package dev.genesshoan.fitnesstrackerapi.workout;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.common.domain.SessionStatus;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ValidationException;
import dev.genesshoan.fitnesstrackerapi.common.mapper.ExerciseMetricsMapper;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.ExerciseQueryUseCase;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.EvaluateAchievementsUseCase;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.UnlockedAchievementResult;
import dev.genesshoan.fitnesstrackerapi.routine.RoutineRepository;
import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.user.infrastructure.persistence.UserRepository;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import dev.genesshoan.fitnesstrackerapi.workout.dto.NotesUpdateRequest;
import dev.genesshoan.fitnesstrackerapi.workout.dto.PositionRequest;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExerciseAddedResponse;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExercisePosition;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionExerciseRequest;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionSetRequest;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionSetResponse;
import dev.genesshoan.fitnesstrackerapi.workout.dto.UnlockedAchievement;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionListItem;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionRequest;
import dev.genesshoan.fitnesstrackerapi.workout.dto.WorkoutSessionResponse;
import dev.genesshoan.fitnesstrackerapi.workout.mapper.SessionExerciseMapper;
import dev.genesshoan.fitnesstrackerapi.workout.mapper.SessionSetMapper;
import dev.genesshoan.fitnesstrackerapi.workout.mapper.WorkoutSessionMapper;
import dev.genesshoan.fitnesstrackerapi.workout.repository.SessionExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.workout.repository.SessionSetRepository;
import dev.genesshoan.fitnesstrackerapi.workout.repository.WorkoutSessionRepository;
import dev.genesshoan.fitnesstrackerapi.workout.repository.projection.LastSetProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages workout sessions, their exercises, and performed sets.
 *
 * <p>Mutating operations lock the relevant session graph and reject changes
 * after completion. New exercise positions are clamped and existing positions
 * are shifted; set numbers are always assigned sequentially by the service.
 */
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
    private final ExerciseQueryUseCase exerciseQueryUseCase;
    private final EvaluateAchievementsUseCase evaluateAchievementsUseCase;

    private final WorkoutSessionMapper workoutSessionMapper;
    private final SessionExerciseMapper sessionExerciseMapper;
    private final SessionSetMapper sessionSetMapper;
    private final ExerciseMetricsMapper exerciseMetricsMapper;

    /** Lists the user's sessions using repository pagination. */
    public Page<WorkoutSessionListItem> getAllWorkoutSessions(UUID userId, Pageable pageable) {

        log.info("Getting workouts sessions from user: {} with pageable: {}", userId, pageable);

        return workoutSessionRepository
                .findAllByUserId(userId, pageable)
                .map(workoutSessionMapper::toWorkoutSessionListItem);
    }

    /** Loads one user-owned session with exercises and sets. */
    public WorkoutSessionResponse getWorkoutSessionById(UUID sessionId, UUID userId) {

        log.info("Getting workout session: {} for user: {}", sessionId, userId);

        WorkoutSession workoutSession = getOrThrowResourceNotFound(
                workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId),
                "Workout session",
                sessionId);

        return toWorkoutSessionResponse(workoutSession, List.of());
    }

    /**
     * Creates an in-progress session from an active routine.
     *
     * <p>Routine exercise positions, metrics, and default set counts are copied.
     */
    @Transactional
    public WorkoutSessionResponse createWorkoutSessionFromRoutine(UUID routineId, UUID userId) {

        Routine routine = getOrThrowResourceNotFound(
                routineRepository.findByIdAndUserIdAndActiveTrue(routineId, userId), "Routine", routineId);

        WorkoutSession session = WorkoutSession.builder()
                .user(userRepository.getReferenceById(userId))
                .routine(routine)
                .startedAt(Instant.now())
                .build();

        routine.getExercises().forEach(re -> {
            SessionExercise exercise = new SessionExercise();

            exercise.setExercise(re.getExercise());
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

        return toWorkoutSessionResponse(saved, List.of());
    }

    /**
     * Creates a session from request data after validating status, timestamps,
     * exercises, and category-specific set metrics.
     *
     * <p>When an exercise has no sets in the request, one set is synthesized
     * from the exercise's last completed set, or its category defaults.
     */
    @Transactional
    public WorkoutSessionResponse createWorkoutSessionFromScratch(WorkoutSessionRequest dto, UUID userId) {

        validateSessionRequest(dto);

        Map<String, List<String>> errors = new HashMap<>();

        Map<UUID, Exercise> exercises = resolveExercises(dto.exercises(), errors);

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

        List<UnlockedAchievementResult> unlocked = List.of();
        if (saved.getStatus() == SessionStatus.COMPLETED) {
            workoutSessionRepository.flush();
            unlocked = evaluateAchievementsUseCase.onSessionCompleted(userId, saved.getId());
        }

        log.info("Created workout session: {} for user: {}", workoutSession.getId(), userId);

        return toWorkoutSessionResponse(saved, unlocked);
    }

    /** Updates notes on an in-progress user-owned session. */
    @Transactional
    public void updateWorkoutSessionNotes(UUID sessionId, UUID userId, NotesUpdateRequest dto) {

        WorkoutSession session = getOrThrowResourceNotFound(
                workoutSessionRepository.findForUpdateByIdAndUserId(sessionId, userId), "Workout session", sessionId);

        if (session.getStatus() == SessionStatus.COMPLETED) {
            log.warn("Invalid workout session notes update attempt: The session {} is already finished", sessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        session.setNotes(dto.notes());
    }

    /**
     * Finishes an in-progress session and assigns {@code completedAt} to the
     * current instant.
     */
    @Transactional
    public WorkoutSessionResponse completeWorkoutSession(UUID sessionId, UUID userId) {

        log.debug("Completing session workout: {} from user: {}", sessionId, userId);

        WorkoutSession session = getOrThrowResourceNotFound(
                workoutSessionRepository.findForUpdateWithExercisesAndCatalogExercise(sessionId, userId),
                "Workout session",
                sessionId);

        if (session.getStatus() == SessionStatus.COMPLETED) {
            log.warn("Invalid workout session finish attempt: The session {} is already finished", sessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        cleanupIncompleteWork(session);
        session.finish();

        workoutSessionRepository.flush();

        List<UnlockedAchievementResult> unlocked = evaluateAchievementsUseCase.onSessionCompleted(userId, sessionId);

        log.info("Completed session workout: {} from user: {}", sessionId, userId);

        return toWorkoutSessionResponse(session, unlocked);
    }

    private void cleanupIncompleteWork(WorkoutSession session) {
        session.getExercises().removeIf(sessionExercise -> {
            sessionExercise.getSets().removeIf(sessionSet -> {
                if (sessionSet.isCompleted()) {
                    return false;
                }

                sessionSet.setSessionExercise(null);
                return true;
            });

            if (!sessionExercise.getSets().isEmpty()) {
                return false;
            }

            sessionExercise.setWorkoutSession(null);
            return true;
        });
    }

    /** Permanently deletes a user-owned workout session and its children. */
    @Transactional
    public void deleteWorkoutSession(UUID sessionId, UUID userId) {

        log.debug("Deleting session workout: {} from user: {}", sessionId, userId);

        WorkoutSession session = getOrThrowResourceNotFound(
                workoutSessionRepository.findForUpdateWithExercises(sessionId, userId), "Workout session", sessionId);

        List<UUID> exerciseIds = session.getExercises().stream()
                .map(sessionExercise -> sessionExercise.getExercise().getId())
                .distinct()
                .toList();

        workoutSessionRepository.delete(session);
        workoutSessionRepository.flush();

        exerciseIds.forEach(exerciseId -> evaluateAchievementsUseCase.refreshExerciseRecords(userId, exerciseId));

        log.info("Deleted session workout: {} from user: {}", sessionId, userId);
    }

    /**
     * Adds an exercise, using the requested position or appending when null.
     * Positions after the insertion point are shifted.
     */
    @Transactional
    public SessionExerciseAddedResponse addNewSessionExercise(UUID sessionId, UUID userId, SessionExerciseRequest dto) {

        log.debug("Adding new session exercise into position: {} to session: {}", dto.position(), sessionId);

        WorkoutSession session = getOrThrowResourceNotFound(
                workoutSessionRepository.findForUpdateWithExercises(sessionId, userId), "Workout session", sessionId);

        if (session.getStatus() == SessionStatus.COMPLETED) {
            log.warn("Invalid session exercise add attempt: The session {} is already finished", sessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        Map<String, List<String>> errors = new HashMap<>();

        Exercise exercise = getOrThrowResourceNotFound(
                exerciseQueryUseCase.findById(dto.exerciseId()), "Exercise", dto.exerciseId());

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

        return sessionExerciseMapper.toSessionExerciseAddedResponse(sessionExercise, shifted);
    }

    /** Updates notes while the parent session remains in progress. */
    @Transactional
    public void updateSessionExerciseNotes(
            UUID workoutSessionId, UUID sessionExerciseId, UUID userId, NotesUpdateRequest dto) {

        SessionExercise sessionExercise = getOrThrowResourceNotFound(
                sessionExerciseRepository.findForUpdateWithWorkoutSession(sessionExerciseId, workoutSessionId, userId),
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

    /**
     * Moves an exercise to a clamped one-based position and returns all
     * resulting positions.
     */
    @Transactional
    public List<SessionExercisePosition> updateSessionExercisePosition(
            UUID workoutSessionId, UUID sessionExerciseId, UUID userId, PositionRequest dto) {

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

        return sessionExerciseMapper.toSessionExercisePositionList(workoutSession.getExercises());
    }

    /** Removes an exercise and closes the position gap. */
    @Transactional
    public List<SessionExercisePosition> deleteSessionExercise(
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

        UUID exerciseId = sessionExercise.getExercise().getId();

        workoutSession.removeExercise(sessionExercise);
        workoutSessionRepository.flush();

        evaluateAchievementsUseCase.refreshExerciseRecords(userId, exerciseId);

        log.info("Session exercise: {} removed successfully from session: {}", sessionExerciseId, workoutSessionId);

        return sessionExerciseMapper.toSessionExercisePositionList(workoutSession.getExercises());
    }

    /**
     * Adds a set with the next sequential number. An empty metrics request
     * reuses the last completed set for the exercise, or category defaults.
     */
    @Transactional
    public SessionSetResponse addNewSessionSet(
            UUID workoutSessionId, UUID sessionExerciseId, UUID userId, SessionSetRequest dto) {

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

        SessionSetRequest effectiveDto =
                dto.isEmpty() ? resolveDefaultsSetForExistingExercise(sessionExercise, userId) : dto;

        if (!dto.isEmpty() && !sessionExercise.getExercise().getCategory().validate(dto.toExerciseMetrics())) {
            throw new BadRequestException("Invalid data for exercise category: "
                    + sessionExercise.getExercise().getCategory());
        }

        SessionSet sessionSet = buildSessionSet(effectiveDto, sessionExercise.getSetCount() + 1);

        sessionExercise.addSet(sessionSet);

        SessionSet saved = sessionSetRepository.save(sessionSet);

        return toSessionSetResponse(saved, List.of());
    }

    /** Updates set metrics and completion state without changing its number. */
    @Transactional
    public SessionSetResponse updateSessionSet(
            UUID workoutSessionId, UUID sessionExerciseId, UUID sessionSetId, UUID userId, SessionSetRequest dto) {

        log.debug(
                "Updating session set: {} from exercise: {}, session: {} and user:{}",
                sessionSetId,
                sessionExerciseId,
                workoutSessionId,
                userId);

        SessionSet sessionSet = getOrThrowResourceNotFound(
                sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
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

        boolean wasCompleted = sessionSet.isCompleted();
        boolean metricsChanged = hasDifferentMetrics(sessionSet, dto);

        applySessionSetUpdate(sessionSet, dto);

        List<UnlockedAchievementResult> unlocked = List.of();

        if (!wasCompleted && sessionSet.isCompleted()) {
            sessionSetRepository.flush();
            unlocked = evaluateAchievementsUseCase.onSetCompleted(userId, workoutSessionId, sessionSetId);
        } else if (wasCompleted && (!sessionSet.isCompleted() || metricsChanged)) {
            sessionSetRepository.flush();
            evaluateAchievementsUseCase.refreshExerciseRecords(
                    userId, sessionSet.getSessionExercise().getExercise().getId());
        }

        return toSessionSetResponse(sessionSet, unlocked);
    }

    /** Marks a set as complete without changing its metrics or number. */
    @Transactional
    public SessionSetResponse markSessionSetComplete(
            UUID workoutSessionId, UUID sessionExerciseId, UUID sessionSetId, UUID userId) {

        log.debug(
                "Marking session set: {} as complete from exercise: {}, session: {} and user: {}",
                sessionSetId,
                sessionExerciseId,
                workoutSessionId,
                userId);

        SessionSet sessionSet = getOrThrowResourceNotFound(
                sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                        sessionSetId, sessionExerciseId, workoutSessionId, userId),
                "Session set",
                sessionSetId);

        if (sessionSet.getSessionExercise().getWorkoutSession().getStatus() == SessionStatus.COMPLETED) {
            log.warn(
                    "Invalid session set {} complete attempt: The session {} is already finished",
                    sessionSetId,
                    workoutSessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        sessionSet.setCompleted(true);

        sessionSetRepository.flush();

        List<UnlockedAchievementResult> unlocked =
                evaluateAchievementsUseCase.onSetCompleted(userId, workoutSessionId, sessionSetId);

        log.info(
                "Session set: {} marked as complete from exercise: {} and session: {}",
                sessionSetId,
                sessionExerciseId,
                workoutSessionId);

        return toSessionSetResponse(sessionSet, unlocked);
    }

    /** Marks a set as incomplete without changing its metrics or number. */
    @Transactional
    public SessionSetResponse markSessionSetIncomplete(
            UUID workoutSessionId, UUID sessionExerciseId, UUID sessionSetId, UUID userId) {

        log.debug(
                "Marking session set: {} as incomplete from exercise: {}, session: {} and user: {}",
                sessionSetId,
                sessionExerciseId,
                workoutSessionId,
                userId);

        SessionSet sessionSet = getOrThrowResourceNotFound(
                sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                        sessionSetId, sessionExerciseId, workoutSessionId, userId),
                "Session set",
                sessionSetId);

        if (sessionSet.getSessionExercise().getWorkoutSession().getStatus() == SessionStatus.COMPLETED) {
            log.warn(
                    "Invalid session set {} incomplete attempt: The session {} is already finished",
                    sessionSetId,
                    workoutSessionId);
            throw new BadRequestException("The workout session is already finished");
        }

        sessionSet.setCompleted(false);

        sessionSetRepository.flush();

        evaluateAchievementsUseCase.refreshExerciseRecords(
                userId, sessionSet.getSessionExercise().getExercise().getId());

        log.info(
                "Session set: {} marked as incomplete from exercise: {} and session: {}",
                sessionSetId,
                sessionExerciseId,
                workoutSessionId);

        return toSessionSetResponse(sessionSet, List.of());
    }

    /** Removes a set, closing its number gap; a sole set removes the exercise. */
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

        boolean wasCompleted = sessionSet.isCompleted();
        UUID exerciseId = sessionExercise.getExercise().getId();

        if (sessionExercise.getSetCount() > 1) {
            sessionExercise.removeSet(sessionSet);
        } else {
            sessionExerciseRepository.delete(sessionExercise);
        }
        sessionExerciseRepository.flush();

        if (wasCompleted) {
            evaluateAchievementsUseCase.refreshExerciseRecords(userId, exerciseId);
        }
    }

    private WorkoutSessionResponse toWorkoutSessionResponse(
            WorkoutSession session, List<UnlockedAchievementResult> unlocked) {

        return workoutSessionMapper.toWorkoutSessionResponse(session, toUnlockedAchievements(unlocked), Map.of());
    }

    private SessionSetResponse toSessionSetResponse(SessionSet sessionSet, List<UnlockedAchievementResult> unlocked) {

        Map<UUID, List<UnlockedAchievement>> achievementsBySetId =
                unlocked.isEmpty() ? Map.of() : Map.of(sessionSet.getId(), toUnlockedAchievements(unlocked));

        return sessionSetMapper.toResponse(sessionSet, achievementsBySetId);
    }

    private List<UnlockedAchievement> toUnlockedAchievements(List<UnlockedAchievementResult> unlocked) {
        return unlocked.stream()
                .map(result -> new UnlockedAchievement(
                        result.id(),
                        result.code(),
                        result.name(),
                        result.tier(),
                        result.exerciseId(),
                        result.unlockedAt()))
                .toList();
    }

    private SessionSetRequest resolveDefaultsSetForExistingExercise(SessionExercise sessionExercise, UUID userId) {
        return sessionExercise.getSets().stream()
                .max(Comparator.comparingInt(SessionSet::getSetNumber))
                .map(ss -> sessionSetMapper.toDefaultSessionSetRequest(ss))
                .orElseGet(() -> exerciseMetricsMapper.toDefaultSessionSetRequest(
                        sessionExercise.getExercise().getCategory().defaultMetrics()));
    }

    private SessionSetRequest resolveDefaultSetForNewExercise(
            Exercise exercise, Map<UUID, LastSetProjection> defaultSets) {

        return Optional.ofNullable(defaultSets.get(exercise.getId()))
                .map(lsp -> new SessionSetRequest(
                        1, lsp.getReps(), lsp.getWeightKg(), lsp.getDurationSeconds(), lsp.getDistanceKm(), false))
                .orElseGet(() -> exerciseMetricsMapper.toDefaultSessionSetRequest(
                        exercise.getCategory().defaultMetrics()));
    }

    private boolean hasDifferentMetrics(SessionSet sessionSet, SessionSetRequest dto) {

        return !Objects.equals(sessionSet.getReps(), dto.reps())
                || !Objects.equals(sessionSet.getWeightKg(), dto.weightKg())
                || !Objects.equals(sessionSet.getDurationSeconds(), dto.durationSeconds())
                || !Objects.equals(sessionSet.getDistanceKm(), dto.distanceKm());
    }

    private void applySessionSetUpdate(SessionSet sessionSet, SessionSetRequest dto) {

        sessionSet.setReps(dto.reps());
        sessionSet.setWeightKg(dto.weightKg());
        sessionSet.setDurationSeconds(dto.durationSeconds());
        sessionSet.setDistanceKm(dto.distanceKm());
        sessionSet.setCompleted(dto.completed());
    }

    /**
     * Resolves the exercises referenced by the request and records an error for
     * every referenced exercise that does not exist.
     *
     * @param exerciseRequests the requested session exercises
     * @param errors       the validation errors to fill
     * @return the existing exercises, keyed by exercise id
     */
    private Map<UUID, Exercise> resolveExercises(
            List<SessionExerciseRequest> exerciseRequests, Map<String, List<String>> errors) {

        List<UUID> exerciseIds = exerciseRequests.stream()
                .map(SessionExerciseRequest::exerciseId)
                .toList();

        if (exerciseIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Exercise> exercises = exerciseQueryUseCase.findByIds(exerciseIds);

        for (UUID exerciseId : new LinkedHashSet<>(exerciseIds)) {
            if (!exercises.containsKey(exerciseId)) {
                errors.computeIfAbsent(exerciseId.toString(), k -> new ArrayList<>())
                        .add("Exercise does not exist");
            }
        }

        return exercises;
    }

    private void validateSetsData(
            List<SessionExerciseRequest> exerciseRequests,
            Map<UUID, Exercise> exercises,
            Map<String, List<String>> errors) {

        for (SessionExerciseRequest exerciseRequest : exerciseRequests) {
            Exercise exercise = exercises.get(exerciseRequest.exerciseId());

            if (exercise != null) {
                validateSetsData(exerciseRequest.sets(), exercise, errors);
            }
        }
    }

    private void validateSetsData(
            List<SessionSetRequest> setRequests, Exercise exercise, Map<String, List<String>> errors) {

        for (int i = 0; i < setRequests.size(); i++) {
            SessionSetRequest set = setRequests.get(i);

            if (!exercise.getCategory().validate(set.toExerciseMetrics())) {
                errors.computeIfAbsent(exercise.getId().toString(), k -> new ArrayList<>())
                        .add("Invalid data for set " + (i + 1) + " for category: " + exercise.getCategory());
            }
        }
    }

    private void buildSessionExercises(
            List<SessionExerciseRequest> exerciseRequests,
            WorkoutSession workoutSession,
            Map<UUID, Exercise> exercises,
            Map<UUID, LastSetProjection> defaultSets,
            UUID userId) {

        for (int i = 0; i < exerciseRequests.size(); i++) {
            SessionExerciseRequest dto = exerciseRequests.get(i);
            Exercise exercise = exercises.get(dto.exerciseId());

            SessionExercise sessionExercise = buildSessionExercise(dto, exercise, defaultSets, userId);

            workoutSession.addExerciseAt(sessionExercise, i + 1);
        }
    }

    private SessionExercise buildSessionExercise(
            SessionExerciseRequest dto, Exercise exercise, Map<UUID, LastSetProjection> defaultSets, UUID userId) {

        SessionExercise sessionExercise =
                SessionExercise.builder().notes(dto.notes()).exercise(exercise).build();

        List<SessionSetRequest> sets =
                dto.sets().isEmpty() ? List.of(resolveDefaultSetForNewExercise(exercise, defaultSets)) : dto.sets();

        for (int i = 0; i < sets.size(); i++) {
            sessionExercise.addSet(buildSessionSet(sets.get(i), i + 1));
        }

        return sessionExercise;
    }

    private SessionSet buildSessionSet(SessionSetRequest dto, int setNumber) {

        return SessionSet.builder()
                .setNumber(setNumber)
                .reps(dto.reps())
                .weightKg(dto.weightKg())
                .durationSeconds(dto.durationSeconds())
                .distanceKm(dto.distanceKm())
                .completed(dto.completed())
                .build();
    }

    private void validateSessionRequest(WorkoutSessionRequest dto) {

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

    private Map<UUID, LastSetProjection> getDefaultsByExercise(List<SessionExerciseRequest> dtos, UUID userId) {
        Set<UUID> exerciseIdsWithoutSets = dtos.stream()
                .filter(eDto -> eDto.sets().isEmpty())
                .map(SessionExerciseRequest::exerciseId)
                .collect(Collectors.toSet());

        if (exerciseIdsWithoutSets.isEmpty()) {
            return Map.of();
        }

        Map<UUID, LastSetProjection> defaultsByExercise =
                sessionSetRepository.findLastSetsByExerciseIdAndUserId(exerciseIdsWithoutSets, userId).stream()
                        .collect(Collectors.toMap(lsp -> lsp.getExerciseId(), Function.identity()));
        return defaultsByExercise;
    }
}
