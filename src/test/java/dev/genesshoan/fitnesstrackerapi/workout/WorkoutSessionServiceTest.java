package dev.genesshoan.fitnesstrackerapi.workout;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import dev.genesshoan.fitnesstrackerapi.common.domain.SessionStatus;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ValidationException;
import dev.genesshoan.fitnesstrackerapi.common.mapper.ExerciseMetricsMapper;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.ExerciseQueryUseCase;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.EvaluateAchievementsUseCase;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.UnlockedAchievementResult;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementTier;
import dev.genesshoan.fitnesstrackerapi.routine.RoutineRepository;
import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.routine.domain.RoutineExercise;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineListItem;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.RoutineBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.RoutineExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionSetBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.UserBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.WorkoutSessionBuilder;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
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
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutSessionServiceTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private WorkoutSessionRepository workoutSessionRepository;

    @Mock
    private SessionExerciseRepository sessionExerciseRepository;

    @Mock
    private SessionSetRepository sessionSetRepository;

    @Mock
    private RoutineRepository routineRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExerciseQueryUseCase exerciseQueryUseCase;

    @Mock
    private WorkoutSessionMapper workoutSessionMapper;

    @Mock
    private SessionExerciseMapper sessionExerciseMapper;

    @Mock
    private SessionSetMapper sessionSetMapper;

    @Mock
    private ExerciseMetricsMapper exerciseMetricsMapper;

    @Mock
    private EvaluateAchievementsUseCase evaluateAchievementsUseCase;

    @InjectMocks
    private WorkoutSessionService workoutSessionService;

    @BeforeEach
    void configureAchievementEvaluation() {
        lenient()
                .when(evaluateAchievementsUseCase.onSessionCompleted(any(UUID.class), any(UUID.class)))
                .thenReturn(List.of());
        lenient()
                .when(evaluateAchievementsUseCase.onSetCompleted(any(UUID.class), any(UUID.class), any(UUID.class)))
                .thenReturn(List.of());
    }

    @Nested
    @DisplayName("addNewSessionExercise")
    class AddNewSessionExercise {
        @Test
        @DisplayName("Should shift existing exercises and return added response when position is in middle")
        void should_shiftExistingExercisesAndReturnAddedResponse_when_positionIsInMiddle() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID exerciseId = UUID.randomUUID();

            User user = UserBuilder.aUser(FAKER).withId(userId).build();
            Exercise exercise = ExerciseBuilder.anExercise(FAKER)
                    .withCategory(Category.STRENGTH)
                    .build();
            exercise.setId(exerciseId);
            SessionExercise first = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withId(UUID.randomUUID())
                    .withPosition(1)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            SessionExercise second = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withId(UUID.randomUUID())
                    .withPosition(2)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(user)
                    .withExercises(List.of(first, second))
                    .build();
            SessionExerciseRequest dto = new SessionExerciseRequest(
                    2, "notes", exerciseId, List.of(new SessionSetRequest(1, 10, 20.0, null, null, true)));
            SessionExerciseAddedResponse response = new SessionExerciseAddedResponse(
                    null,
                    List.of(
                            new SessionExercisePosition(first.getId(), 1),
                            new SessionExercisePosition(second.getId(), 3)));

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));
            when(exerciseQueryUseCase.findById(exerciseId)).thenReturn(Optional.of(exercise));
            when(sessionExerciseMapper.toSessionExerciseAddedResponse(any(SessionExercise.class), any()))
                    .thenReturn(response);

            SessionExerciseAddedResponse result = workoutSessionService.addNewSessionExercise(sessionId, userId, dto);

            ArgumentCaptor<SessionExercise> addedExerciseCaptor = ArgumentCaptor.forClass(SessionExercise.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SessionExercise>> shiftedCaptor = ArgumentCaptor.forClass(List.class);

            verify(sessionExerciseMapper)
                    .toSessionExerciseAddedResponse(addedExerciseCaptor.capture(), shiftedCaptor.capture());

            SessionExercise addedExercise = addedExerciseCaptor.getValue();
            List<SessionExercise> shiftedExercises = shiftedCaptor.getValue();

            assertThat(result).isEqualTo(response);
            assertThat(session.getExercises()).hasSize(3);
            assertThat(session.getExercises())
                    .extracting(SessionExercise::getPosition)
                    .containsExactly(1, 3, 2);
            assertThat(addedExercise.getPosition()).isEqualTo(2);
            assertThat(addedExercise.getNotes()).isEqualTo("notes");
            assertThat(addedExercise.getExercise()).isEqualTo(exercise);
            assertThat(addedExercise.getSets()).hasSize(1);
            assertThat(addedExercise.getSets().get(0).getSetNumber()).isEqualTo(1);
            assertThat(addedExercise.getSets().get(0).getReps()).isEqualTo(10);
            assertThat(addedExercise.getSets().get(0).getWeightKg()).isEqualTo(20.0);
            assertThat(shiftedExercises)
                    .extracting(SessionExercise::getId)
                    .containsExactly(first.getId(), second.getId());
            assertThat(shiftedExercises)
                    .extracting(SessionExercise::getPosition)
                    .containsExactly(1, 3);
            verify(workoutSessionRepository).findForUpdateWithExercises(sessionId, userId);
            verify(exerciseQueryUseCase).findById(exerciseId);
        }

        @Test
        @DisplayName("Should throw bad request exception when session is completed")
        void should_throwBadRequestException_when_sessionIsCompleted() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .withStatus(SessionStatus.COMPLETED)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .build();

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));

            assertThatThrownBy(() -> workoutSessionService.addNewSessionExercise(
                            sessionId,
                            userId,
                            new SessionExerciseRequest(
                                    1,
                                    "notes",
                                    UUID.randomUUID(),
                                    List.of(new SessionSetRequest(1, 10, 20.0, null, null, true)))))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The workout session is already finished");

            verify(workoutSessionRepository).findForUpdateWithExercises(sessionId, userId);
            verifyNoInteractions(exerciseQueryUseCase, sessionExerciseMapper, sessionSetMapper, sessionSetRepository);
        }

        @Test
        @DisplayName("Should throw resource not found exception when session is missing")
        void should_throwResourceNotFoundException_when_sessionIsMissing() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> workoutSessionService.addNewSessionExercise(
                            sessionId,
                            userId,
                            new SessionExerciseRequest(
                                    1,
                                    "notes",
                                    UUID.randomUUID(),
                                    List.of(new SessionSetRequest(1, 10, 20.0, null, null, true)))))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Workout session not found");
        }

        @Test
        @DisplayName("Should throw validation exception when set data does not match category")
        void should_throwValidationException_when_setDataDoesNotMatchCategory() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID exerciseId = UUID.randomUUID();
            User user = UserBuilder.aUser(FAKER).withId(userId).build();
            Exercise exercise = ExerciseBuilder.anExercise(FAKER)
                    .withCategory(Category.STRENGTH)
                    .build();
            exercise.setId(exerciseId);
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(user)
                    .build();
            SessionExerciseRequest dto = new SessionExerciseRequest(
                    1, "notes", exerciseId, List.of(new SessionSetRequest(1, null, null, 5, null, true)));

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));
            when(exerciseQueryUseCase.findById(exerciseId)).thenReturn(Optional.of(exercise));

            assertThatThrownBy(() -> workoutSessionService.addNewSessionExercise(sessionId, userId, dto))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                            .containsEntry(
                                    exerciseId.toString(), List.of("Invalid data for set 1 for category: STRENGTH")));
            verifyNoInteractions(sessionExerciseMapper, sessionSetRepository);
        }

        @Test
        @DisplayName("Should throw resource not found exception when exercise does not exist")
        void should_throwResourceNotFoundException_when_exerciseDoesNotExist() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID exerciseId = UUID.randomUUID();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .build();

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));
            when(exerciseQueryUseCase.findById(exerciseId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workoutSessionService.addNewSessionExercise(
                            sessionId,
                            userId,
                            new SessionExerciseRequest(
                                    1,
                                    "notes",
                                    exerciseId,
                                    List.of(new SessionSetRequest(1, 10, 20.0, null, null, true)))))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Exercise not found");
        }

        @Test
        @DisplayName("Should append exercise at the end when position is null")
        void should_appendExerciseAtTheEnd_when_positionIsNull() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID exerciseId = UUID.randomUUID();

            Exercise exercise = ExerciseBuilder.anExercise(FAKER)
                    .withCategory(Category.STRENGTH)
                    .build();
            exercise.setId(exerciseId);
            SessionExercise first = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withId(UUID.randomUUID())
                    .withPosition(1)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(first))
                    .build();
            SessionExerciseRequest dto = new SessionExerciseRequest(
                    null, "notes", exerciseId, List.of(new SessionSetRequest(1, 10, 20.0, null, null, true)));

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));
            when(exerciseQueryUseCase.findById(exerciseId)).thenReturn(Optional.of(exercise));

            workoutSessionService.addNewSessionExercise(sessionId, userId, dto);

            ArgumentCaptor<SessionExercise> addedExerciseCaptor = ArgumentCaptor.forClass(SessionExercise.class);
            verify(sessionExerciseMapper).toSessionExerciseAddedResponse(addedExerciseCaptor.capture(), any());

            assertThat(session.getExercises()).hasSize(2);
            assertThat(addedExerciseCaptor.getValue().getPosition()).isEqualTo(2);
            assertThat(session.getExercises())
                    .extracting(SessionExercise::getPosition)
                    .containsExactly(1, 2);
        }
    }

    @Nested
    @DisplayName("updateSessionExercisePosition")
    class UpdateSessionExercisePosition {
        @Test
        @DisplayName("Should shift exercises down when moving exercise up")
        void should_shiftExercisesDownWhenMovingExerciseUp() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            SessionExercise first = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withPosition(1)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            SessionExercise second = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withPosition(2)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            SessionExercise third = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withPosition(3)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(first, second, third))
                    .build();

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));
            List<SessionExercisePosition> response = List.of(
                    new SessionExercisePosition(second.getId(), 1),
                    new SessionExercisePosition(first.getId(), 2),
                    new SessionExercisePosition(third.getId(), 3));
            when(sessionExerciseMapper.toSessionExercisePositionList(any())).thenReturn(response);

            List<SessionExercisePosition> result = workoutSessionService.updateSessionExercisePosition(
                    sessionId, second.getId(), userId, new PositionRequest(1));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SessionExercise>> mapperInputCaptor = ArgumentCaptor.forClass(List.class);
            verify(sessionExerciseMapper).toSessionExercisePositionList(mapperInputCaptor.capture());

            assertThat(result).isEqualTo(response);
            assertThat(first.getPosition()).isEqualTo(2);
            assertThat(second.getPosition()).isEqualTo(1);
            assertThat(third.getPosition()).isEqualTo(3);
            assertThat(mapperInputCaptor.getValue())
                    .extracting(SessionExercise::getId)
                    .containsExactly(second.getId(), first.getId(), third.getId());
        }

        @Test
        @DisplayName("Should shift exercises up when moving exercise down")
        void should_shiftExercisesUpWhenMovingExerciseDown() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            SessionExercise first = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withPosition(1)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            SessionExercise second = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withPosition(2)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            SessionExercise third = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withPosition(3)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(first, second, third))
                    .build();

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));
            List<SessionExercisePosition> response = List.of(
                    new SessionExercisePosition(second.getId(), 1),
                    new SessionExercisePosition(third.getId(), 2),
                    new SessionExercisePosition(first.getId(), 3));
            when(sessionExerciseMapper.toSessionExercisePositionList(any())).thenReturn(response);

            List<SessionExercisePosition> result = workoutSessionService.updateSessionExercisePosition(
                    sessionId, first.getId(), userId, new PositionRequest(3));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SessionExercise>> mapperInputCaptor = ArgumentCaptor.forClass(List.class);
            verify(sessionExerciseMapper).toSessionExercisePositionList(mapperInputCaptor.capture());

            assertThat(result).isEqualTo(response);
            assertThat(first.getPosition()).isEqualTo(3);
            assertThat(second.getPosition()).isEqualTo(1);
            assertThat(third.getPosition()).isEqualTo(2);
            assertThat(mapperInputCaptor.getValue())
                    .extracting(SessionExercise::getId)
                    .containsExactly(second.getId(), third.getId(), first.getId());
        }

        @Test
        @DisplayName("Should keep positions unchanged when moving to same position")
        void should_keepPositionsUnchanged_when_movingToSamePosition() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            SessionExercise first = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withPosition(1)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            SessionExercise second = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withPosition(2)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(first, second))
                    .build();
            List<SessionExercisePosition> response = List.of(
                    new SessionExercisePosition(first.getId(), 1), new SessionExercisePosition(second.getId(), 2));

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));
            when(sessionExerciseMapper.toSessionExercisePositionList(any())).thenReturn(response);

            List<SessionExercisePosition> result = workoutSessionService.updateSessionExercisePosition(
                    sessionId, second.getId(), userId, new PositionRequest(2));

            assertThat(result).isEqualTo(response);
            assertThat(first.getPosition()).isEqualTo(1);
            assertThat(second.getPosition()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should throw bad request exception when session is completed")
        void should_throwBadRequestException_when_sessionIsCompleted() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            SessionExercise first = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .withStatus(SessionStatus.COMPLETED)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(first))
                    .build();

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));

            assertThatThrownBy(() -> workoutSessionService.updateSessionExercisePosition(
                            sessionId, first.getId(), userId, new PositionRequest(1)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The workout session is already finished");
        }

        @Test
        @DisplayName("Should throw resource not found exception when session exercise does not exist")
        void should_throwResourceNotFoundException_when_sessionExerciseDoesNotExist() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .build();

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));

            assertThatThrownBy(() -> workoutSessionService.updateSessionExercisePosition(
                            sessionId, UUID.randomUUID(), userId, new PositionRequest(1)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Session exercise not found");
        }
    }

    @Nested
    @DisplayName("deleteSessionExercise")
    class DeleteSessionExercise {
        @Test
        @DisplayName("Should close position gap and return updated positions when exercise is removed")
        void should_closePositionGapAndReturnUpdatedPositions_when_exerciseIsRemoved() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            SessionExercise first = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withPosition(1)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            SessionExercise second = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withPosition(2)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            SessionExercise third = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withPosition(3)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(first, second, third))
                    .build();

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));
            List<SessionExercisePosition> response = List.of(
                    new SessionExercisePosition(first.getId(), 1), new SessionExercisePosition(third.getId(), 2));
            when(sessionExerciseMapper.toSessionExercisePositionList(any())).thenReturn(response);

            List<SessionExercisePosition> result =
                    workoutSessionService.deleteSessionExercise(sessionId, second.getId(), userId);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SessionExercise>> mapperInputCaptor = ArgumentCaptor.forClass(List.class);
            verify(sessionExerciseMapper).toSessionExercisePositionList(mapperInputCaptor.capture());

            assertThat(result).isEqualTo(response);
            assertThat(session.getExercises())
                    .extracting(SessionExercise::getPosition)
                    .containsExactly(1, 2);
            assertThat(mapperInputCaptor.getValue())
                    .extracting(SessionExercise::getId)
                    .containsExactly(first.getId(), third.getId());
        }

        @Test
        @DisplayName("Should throw bad request exception when deleting exercise from completed session")
        void should_throwBadRequestException_when_deletingExerciseFromCompletedSession() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            SessionExercise first = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withPosition(1)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .withStatus(SessionStatus.COMPLETED)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(first))
                    .build();

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));

            assertThatThrownBy(() -> workoutSessionService.deleteSessionExercise(sessionId, first.getId(), userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The workout session is already finished");
        }

        @Test
        @DisplayName("Should throw resource not found exception when deleting exercise that does not exist in session")
        void should_throwResourceNotFoundException_when_deletingExerciseThatDoesNotExistInSession() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .build();

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));

            assertThatThrownBy(() -> workoutSessionService.deleteSessionExercise(sessionId, UUID.randomUUID(), userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Session exercise not found");
        }
    }

    @Nested
    @DisplayName("addNewSessionSet")
    class AddNewSessionSet {
        @Test
        @DisplayName("Should append set and return mapped response when session is active")
        void should_appendSetAndReturnMappedResponse_when_sessionIsActive() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            SessionSet existingSet = SessionSetBuilder.aSessionSet(FAKER)
                    .withSetNumber(1)
                    .withReps(10)
                    .withWeightKg(15.0)
                    .withCompleted(true)
                    .build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withSets(List.of(existingSet))
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            SessionSetRequest dto = new SessionSetRequest(null, 8, 20.0, null, null, true);

            when(sessionExerciseRepository.findForUpdateWithWorkoutSessionAndExerciseAndSets(
                            sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));
            when(sessionSetRepository.save(any(SessionSet.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(sessionSetMapper.toResponse(any(SessionSet.class), anyMap())).thenAnswer(invocation -> {
                SessionSet set = invocation.getArgument(0);
                return new SessionSetResponse(
                        set.getId(),
                        set.getSetNumber(),
                        set.getReps(),
                        set.getWeightKg(),
                        set.getDurationSeconds(),
                        set.getDistanceKm(),
                        set.isCompleted(),
                        List.of());
            });

            SessionSetResponse result =
                    workoutSessionService.addNewSessionSet(sessionId, sessionExercise.getId(), userId, dto);

            ArgumentCaptor<SessionSet> savedSetCaptor = ArgumentCaptor.forClass(SessionSet.class);
            verify(sessionSetRepository).save(savedSetCaptor.capture());

            SessionSet savedSet = savedSetCaptor.getValue();
            assertThat(savedSet.getSetNumber()).isEqualTo(2);
            assertThat(savedSet.getReps()).isEqualTo(8);
            assertThat(savedSet.getWeightKg()).isEqualTo(20.0);
            assertThat(savedSet.getDurationSeconds()).isNull();
            assertThat(savedSet.getDistanceKm()).isNull();
            assertThat(savedSet.isCompleted()).isTrue();
            assertThat(savedSet.getSessionExercise()).isEqualTo(sessionExercise);
            assertThat(sessionExercise.getSets()).hasSize(2);
            assertThat(result.setNumber()).isEqualTo(savedSet.getSetNumber());
            assertThat(result.reps()).isEqualTo(savedSet.getReps());
            assertThat(result.weightKg()).isEqualTo(savedSet.getWeightKg());
            assertThat(result.completed()).isEqualTo(savedSet.isCompleted());
        }

        @Test
        @DisplayName("Should throw bad request exception when session is completed")
        void should_throwBadRequestException_when_sessionIsCompleted() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .withStatus(SessionStatus.COMPLETED)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            sessionExercise.setWorkoutSession(session);

            when(sessionExerciseRepository.findForUpdateWithWorkoutSessionAndExerciseAndSets(
                            sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));

            assertThatThrownBy(() -> workoutSessionService.addNewSessionSet(
                            sessionId,
                            sessionExercise.getId(),
                            userId,
                            new SessionSetRequest(null, 8, 20.0, null, null, true)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The workout session is already finished");

            verify(sessionSetRepository, never()).save(any(SessionSet.class));
            verifyNoInteractions(sessionSetMapper);
        }

        @Test
        @DisplayName("Should reuse the last set metrics when the request has no metrics")
        void should_reuseLastSetMetrics_when_requestHasNoMetrics() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            SessionSet lastSet = SessionSetBuilder.aSessionSet(FAKER)
                    .withSetNumber(2)
                    .withReps(12)
                    .withWeightKg(30.0)
                    .withCompleted(true)
                    .build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withSets(List.of(lastSet))
                    .forExercise(ExerciseBuilder.anExercise(FAKER)
                            .withCategory(Category.STRENGTH)
                            .build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            SessionSetRequest dto = new SessionSetRequest(null, null, null, null, null, false);

            when(sessionExerciseRepository.findForUpdateWithWorkoutSessionAndExerciseAndSets(
                            sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));
            when(sessionSetMapper.toDefaultSessionSetRequest(lastSet))
                    .thenReturn(new SessionSetRequest(1, 12, 30.0, null, null, false));
            when(sessionSetRepository.save(any(SessionSet.class))).thenAnswer(invocation -> invocation.getArgument(0));

            workoutSessionService.addNewSessionSet(sessionId, sessionExercise.getId(), userId, dto);

            ArgumentCaptor<SessionSet> savedSetCaptor = ArgumentCaptor.forClass(SessionSet.class);
            verify(sessionSetRepository).save(savedSetCaptor.capture());

            SessionSet savedSet = savedSetCaptor.getValue();
            assertThat(savedSet.getSetNumber()).isEqualTo(2);
            assertThat(savedSet.getReps()).isEqualTo(12);
            assertThat(savedSet.getWeightKg()).isEqualTo(30.0);
            assertThat(savedSet.isCompleted()).isFalse();
            verify(sessionSetMapper).toDefaultSessionSetRequest(lastSet);
            verifyNoInteractions(exerciseMetricsMapper);
        }

        @Test
        @DisplayName("Should use the category defaults when the exercise has no sets")
        void should_useCategoryDefaults_when_exerciseHasNoSets() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .forExercise(ExerciseBuilder.anExercise(FAKER)
                            .withCategory(Category.STRENGTH)
                            .build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            SessionSetRequest dto = new SessionSetRequest(null, null, null, null, null, false);

            when(sessionExerciseRepository.findForUpdateWithWorkoutSessionAndExerciseAndSets(
                            sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));
            when(exerciseMetricsMapper.toDefaultSessionSetRequest(Category.STRENGTH.defaultMetrics()))
                    .thenReturn(new SessionSetRequest(1, 8, 5.0, null, null, false));
            when(sessionSetRepository.save(any(SessionSet.class))).thenAnswer(invocation -> invocation.getArgument(0));

            workoutSessionService.addNewSessionSet(sessionId, sessionExercise.getId(), userId, dto);

            ArgumentCaptor<SessionSet> savedSetCaptor = ArgumentCaptor.forClass(SessionSet.class);
            verify(sessionSetRepository).save(savedSetCaptor.capture());

            SessionSet savedSet = savedSetCaptor.getValue();
            assertThat(savedSet.getSetNumber()).isEqualTo(1);
            assertThat(savedSet.getReps()).isEqualTo(8);
            assertThat(savedSet.getWeightKg()).isEqualTo(5.0);
            verify(exerciseMetricsMapper).toDefaultSessionSetRequest(Category.STRENGTH.defaultMetrics());
            verify(sessionSetMapper, never()).toDefaultSessionSetRequest(any(SessionSet.class));
        }
    }

    @Nested
    @DisplayName("updateSessionSet")
    class UpdateSessionSet {
        @Test
        @DisplayName("Should update set fields and return mapped response when session is active")
        void should_updateSetFieldsAndReturnMappedResponse_when_sessionIsActive() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();
            SessionSet sessionSet = SessionSetBuilder.aSessionSet(FAKER)
                    .withId(sessionSetId)
                    .withSetNumber(1)
                    .withReps(10)
                    .withWeightKg(15.0)
                    .withCompleted(false)
                    .build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withSets(List.of(sessionSet))
                    .forExercise(ExerciseBuilder.anExercise(FAKER)
                            .withCategory(Category.STRENGTH)
                            .build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            sessionSet.setSessionExercise(sessionExercise);
            sessionExercise.setWorkoutSession(session);
            SessionSetRequest dto = new SessionSetRequest(1, 12, 20.0, null, null, true);

            when(sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                            sessionSetId, sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionSet));
            when(sessionSetMapper.toResponse(eq(sessionSet), anyMap())).thenAnswer(invocation -> {
                SessionSet set = invocation.getArgument(0);
                return new SessionSetResponse(
                        set.getId(),
                        set.getSetNumber(),
                        set.getReps(),
                        set.getWeightKg(),
                        set.getDurationSeconds(),
                        set.getDistanceKm(),
                        set.isCompleted(),
                        List.of());
            });

            SessionSetResponse result = workoutSessionService.updateSessionSet(
                    sessionId, sessionExercise.getId(), sessionSetId, userId, dto);

            assertThat(sessionSet.getReps()).isEqualTo(12);
            assertThat(sessionSet.getWeightKg()).isEqualTo(20.0);
            assertThat(sessionSet.getDurationSeconds()).isNull();
            assertThat(sessionSet.getDistanceKm()).isNull();
            assertThat(sessionSet.isCompleted()).isTrue();
            assertThat(result.setNumber()).isEqualTo(1);
            assertThat(result.reps()).isEqualTo(12);
            assertThat(result.weightKg()).isEqualTo(20.0);
            assertThat(result.completed()).isTrue();
        }

        @Test
        @DisplayName("Should throw bad request exception when session is completed")
        void should_throwBadRequestException_when_sessionIsCompleted() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();
            SessionSet sessionSet =
                    SessionSetBuilder.aSessionSet(FAKER).withId(sessionSetId).build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withSets(List.of(sessionSet))
                    .forExercise(ExerciseBuilder.anExercise(FAKER)
                            .withCategory(Category.STRENGTH)
                            .build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .withStatus(SessionStatus.COMPLETED)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            sessionSet.setSessionExercise(sessionExercise);
            sessionExercise.setWorkoutSession(session);

            when(sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                            sessionSetId, sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionSet));

            assertThatThrownBy(() -> workoutSessionService.updateSessionSet(
                            sessionId,
                            sessionExercise.getId(),
                            sessionSetId,
                            userId,
                            new SessionSetRequest(1, 12, 20.0, null, null, true)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The workout session is already finished");
        }

        @Test
        @DisplayName("Should throw bad request exception when set data does not match category")
        void should_throwBadRequestException_when_setDataDoesNotMatchCategory() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();
            SessionSet sessionSet =
                    SessionSetBuilder.aSessionSet(FAKER).withId(sessionSetId).build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withSets(List.of(sessionSet))
                    .forExercise(ExerciseBuilder.anExercise(FAKER)
                            .withCategory(Category.STRENGTH)
                            .build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            sessionSet.setSessionExercise(sessionExercise);
            sessionExercise.setWorkoutSession(session);

            when(sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                            sessionSetId, sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionSet));

            assertThatThrownBy(() -> workoutSessionService.updateSessionSet(
                            sessionId,
                            sessionExercise.getId(),
                            sessionSetId,
                            userId,
                            new SessionSetRequest(1, null, null, 5, null, true)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Invalid data for exercise category: STRENGTH");
            verifyNoInteractions(sessionSetMapper);
        }

        @Test
        @DisplayName("Should throw resource not found exception when session set is missing")
        void should_throwResourceNotFoundException_when_sessionSetIsMissing() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionExerciseId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();

            when(sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                            sessionSetId, sessionExerciseId, sessionId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> workoutSessionService.updateSessionSet(
                            sessionId,
                            sessionExerciseId,
                            sessionSetId,
                            userId,
                            new SessionSetRequest(1, 12, 20.0, null, null, true)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Session set not found");
        }
    }

    @Nested
    @DisplayName("markSessionSetIncomplete")
    class MarkSessionSetIncomplete {
        @Test
        @DisplayName("Should mark a completed set as incomplete and return mapped response")
        void should_markCompletedSetAsIncomplete_when_sessionIsActive() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();
            SessionSet sessionSet = SessionSetBuilder.aSessionSet(FAKER)
                    .withId(sessionSetId)
                    .withSetNumber(1)
                    .withReps(10)
                    .withWeightKg(15.0)
                    .withCompleted(true)
                    .build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withSets(List.of(sessionSet))
                    .forExercise(ExerciseBuilder.anExercise(FAKER)
                            .withCategory(Category.STRENGTH)
                            .build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            sessionSet.setSessionExercise(sessionExercise);
            sessionExercise.setWorkoutSession(session);

            when(sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                            sessionSetId, sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionSet));
            when(sessionSetMapper.toResponse(eq(sessionSet), anyMap())).thenAnswer(invocation -> {
                SessionSet set = invocation.getArgument(0);
                return new SessionSetResponse(
                        set.getId(),
                        set.getSetNumber(),
                        set.getReps(),
                        set.getWeightKg(),
                        set.getDurationSeconds(),
                        set.getDistanceKm(),
                        set.isCompleted(),
                        List.of());
            });

            SessionSetResponse result = workoutSessionService.markSessionSetIncomplete(
                    sessionId, sessionExercise.getId(), sessionSetId, userId);

            assertThat(sessionSet.isCompleted()).isFalse();
            assertThat(sessionSet.getReps()).isEqualTo(10);
            assertThat(sessionSet.getWeightKg()).isEqualTo(15.0);
            assertThat(result.setNumber()).isEqualTo(1);
            assertThat(result.reps()).isEqualTo(10);
            assertThat(result.completed()).isFalse();
            verify(evaluateAchievementsUseCase)
                    .refreshExerciseRecords(
                            eq(userId), eq(sessionExercise.getExercise().getId()));
        }

        @Test
        @DisplayName("Should throw bad request exception when session is completed")
        void should_throwBadRequestException_when_sessionIsCompleted() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();
            SessionSet sessionSet = SessionSetBuilder.aSessionSet(FAKER)
                    .withId(sessionSetId)
                    .withCompleted(true)
                    .build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withSets(List.of(sessionSet))
                    .forExercise(ExerciseBuilder.anExercise(FAKER)
                            .withCategory(Category.STRENGTH)
                            .build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .withStatus(SessionStatus.COMPLETED)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            sessionSet.setSessionExercise(sessionExercise);
            sessionExercise.setWorkoutSession(session);

            when(sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                            sessionSetId, sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionSet));

            assertThatThrownBy(() -> workoutSessionService.markSessionSetIncomplete(
                            sessionId, sessionExercise.getId(), sessionSetId, userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The workout session is already finished");

            assertThat(sessionSet.isCompleted()).isTrue();
            verifyNoInteractions(sessionSetMapper);
        }

        @Test
        @DisplayName("Should throw resource not found exception when session set is missing")
        void should_throwResourceNotFoundException_when_sessionSetIsMissing() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionExerciseId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();

            when(sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                            sessionSetId, sessionExerciseId, sessionId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> workoutSessionService.markSessionSetIncomplete(
                            sessionId, sessionExerciseId, sessionSetId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Session set not found");

            verifyNoInteractions(sessionSetMapper);
        }
    }

    @Nested
    @DisplayName("markSessionSetComplete")
    class MarkSessionSetComplete {
        @Test
        @DisplayName("Should mark an incomplete set as complete and return mapped response")
        void should_markIncompleteSetAsComplete_when_sessionIsActive() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();
            SessionSet sessionSet = SessionSetBuilder.aSessionSet(FAKER)
                    .withId(sessionSetId)
                    .withSetNumber(1)
                    .withReps(10)
                    .withWeightKg(15.0)
                    .withCompleted(false)
                    .build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withSets(List.of(sessionSet))
                    .forExercise(ExerciseBuilder.anExercise(FAKER)
                            .withCategory(Category.STRENGTH)
                            .build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            sessionSet.setSessionExercise(sessionExercise);
            sessionExercise.setWorkoutSession(session);

            when(sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                            sessionSetId, sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionSet));
            when(sessionSetMapper.toResponse(eq(sessionSet), anyMap())).thenAnswer(invocation -> {
                SessionSet set = invocation.getArgument(0);
                return new SessionSetResponse(
                        set.getId(),
                        set.getSetNumber(),
                        set.getReps(),
                        set.getWeightKg(),
                        set.getDurationSeconds(),
                        set.getDistanceKm(),
                        set.isCompleted(),
                        List.of());
            });

            SessionSetResponse result = workoutSessionService.markSessionSetComplete(
                    sessionId, sessionExercise.getId(), sessionSetId, userId);

            assertThat(sessionSet.isCompleted()).isTrue();
            assertThat(sessionSet.getReps()).isEqualTo(10);
            assertThat(sessionSet.getWeightKg()).isEqualTo(15.0);
            assertThat(result.setNumber()).isEqualTo(1);
            assertThat(result.reps()).isEqualTo(10);
            assertThat(result.completed()).isTrue();
            verify(evaluateAchievementsUseCase).onSetCompleted(eq(userId), eq(sessionId), eq(sessionSetId));
        }

        @Test
        @DisplayName("Should throw bad request exception when session is completed")
        void should_throwBadRequestException_when_sessionIsCompleted() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();
            SessionSet sessionSet = SessionSetBuilder.aSessionSet(FAKER)
                    .withId(sessionSetId)
                    .withCompleted(false)
                    .build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withSets(List.of(sessionSet))
                    .forExercise(ExerciseBuilder.anExercise(FAKER)
                            .withCategory(Category.STRENGTH)
                            .build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .withStatus(SessionStatus.COMPLETED)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            sessionSet.setSessionExercise(sessionExercise);
            sessionExercise.setWorkoutSession(session);

            when(sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                            sessionSetId, sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionSet));

            assertThatThrownBy(() -> workoutSessionService.markSessionSetComplete(
                            sessionId, sessionExercise.getId(), sessionSetId, userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The workout session is already finished");

            assertThat(sessionSet.isCompleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("deleteSessionSet")
    class DeleteSessionSet {
        @Test
        @DisplayName("Should close set number gap and remove set when session is active")
        void should_closeSetNumberGapAndRemoveSet_when_sessionIsActive() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();
            SessionSet firstSet = SessionSetBuilder.aSessionSet(FAKER)
                    .withId(sessionSetId)
                    .withSetNumber(1)
                    .build();
            SessionSet secondSet =
                    SessionSetBuilder.aSessionSet(FAKER).withSetNumber(2).build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withSets(List.of(firstSet, secondSet))
                    .forExercise(ExerciseBuilder.anExercise(FAKER)
                            .withCategory(Category.STRENGTH)
                            .build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            firstSet.setSessionExercise(sessionExercise);
            secondSet.setSessionExercise(sessionExercise);
            sessionExercise.setWorkoutSession(session);

            when(sessionExerciseRepository.findForUpdateWithWorkoutSessionAndSets(
                            sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));

            workoutSessionService.deleteSessionSet(sessionId, sessionExercise.getId(), sessionSetId, userId);

            assertThat(sessionExercise.getSets()).hasSize(1);
            assertThat(sessionExercise.getSets().get(0).getSetNumber()).isEqualTo(1);
            assertThat(sessionExercise.getSets().get(0).getId()).isEqualTo(secondSet.getId());
            assertThat(firstSet.getSessionExercise()).isNull();
        }

        @Test
        @DisplayName("Should throw bad request exception when session is completed")
        void should_throwBadRequestException_when_sessionIsCompleted() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();
            SessionSet firstSet = SessionSetBuilder.aSessionSet(FAKER)
                    .withId(sessionSetId)
                    .withSetNumber(1)
                    .build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withSets(List.of(firstSet))
                    .forExercise(ExerciseBuilder.anExercise(FAKER)
                            .withCategory(Category.STRENGTH)
                            .build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .withStatus(SessionStatus.COMPLETED)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            firstSet.setSessionExercise(sessionExercise);
            sessionExercise.setWorkoutSession(session);

            when(sessionExerciseRepository.findForUpdateWithWorkoutSessionAndSets(
                            sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));

            assertThatThrownBy(() -> workoutSessionService.deleteSessionSet(
                            sessionId, sessionExercise.getId(), sessionSetId, userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The workout session is already finished");
        }

        @Test
        @DisplayName("Should throw resource not found exception when session exercise is missing")
        void should_throwResourceNotFoundException_when_sessionExerciseIsMissing() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionExerciseId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();

            when(sessionExerciseRepository.findForUpdateWithWorkoutSessionAndSets(sessionExerciseId, sessionId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                            workoutSessionService.deleteSessionSet(sessionId, sessionExerciseId, sessionSetId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Session exercise not found");
        }

        @Test
        @DisplayName("Should throw resource not found exception when session set is missing from exercise")
        void should_throwResourceNotFoundException_when_sessionSetIsMissingFromExercise() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .forExercise(ExerciseBuilder.anExercise(FAKER)
                            .withCategory(Category.STRENGTH)
                            .build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            sessionExercise.setWorkoutSession(session);

            when(sessionExerciseRepository.findForUpdateWithWorkoutSessionAndSets(
                            sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));

            assertThatThrownBy(() -> workoutSessionService.deleteSessionSet(
                            sessionId, sessionExercise.getId(), sessionSetId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Session set not found");
        }

        @Test
        @DisplayName("Should delete the session exercise when the deleted set is the only one")
        void should_deleteSessionExercise_when_itIsTheOnlySet() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();
            SessionSet onlySet = SessionSetBuilder.aSessionSet(FAKER)
                    .withId(sessionSetId)
                    .withSetNumber(1)
                    .build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withSets(List.of(onlySet))
                    .forExercise(ExerciseBuilder.anExercise(FAKER)
                            .withCategory(Category.STRENGTH)
                            .build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            onlySet.setSessionExercise(sessionExercise);
            sessionExercise.setWorkoutSession(session);

            when(sessionExerciseRepository.findForUpdateWithWorkoutSessionAndSets(
                            sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));

            workoutSessionService.deleteSessionSet(sessionId, sessionExercise.getId(), sessionSetId, userId);

            verify(sessionExerciseRepository).delete(sessionExercise);
            assertThat(sessionExercise.getSets()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("updateSessionExerciseNotes")
    class UpdateSessionExerciseNotes {
        @Test
        @DisplayName("Should update exercise notes when session is active")
        void should_updateExerciseNotes_when_sessionIsActive() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionExerciseId = UUID.randomUUID();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withId(sessionExerciseId)
                    .withNotes("old")
                    .forWorkoutSession(session)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();

            when(sessionExerciseRepository.findForUpdateWithWorkoutSession(sessionExerciseId, sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));

            workoutSessionService.updateSessionExerciseNotes(
                    sessionId, sessionExerciseId, userId, new NotesUpdateRequest("new"));

            assertThat(sessionExercise.getNotes()).isEqualTo("new");
        }

        @Test
        @DisplayName("Should throw bad request exception when updating exercise notes in completed session")
        void should_throwBadRequestException_when_updatingExerciseNotesInCompletedSession() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionExerciseId = UUID.randomUUID();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .withStatus(SessionStatus.COMPLETED)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withId(sessionExerciseId)
                    .forWorkoutSession(session)
                    .forExercise(ExerciseBuilder.anExercise(FAKER).build())
                    .build();

            when(sessionExerciseRepository.findForUpdateWithWorkoutSession(sessionExerciseId, sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));

            assertThatThrownBy(() -> workoutSessionService.updateSessionExerciseNotes(
                            sessionId, sessionExerciseId, userId, new NotesUpdateRequest("new")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The workout session is already finished");
        }

        @Test
        @DisplayName("Should throw resource not found exception when updating notes for missing session exercise")
        void should_throwResourceNotFoundException_when_updatingNotesForMissingSessionExercise() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionExerciseId = UUID.randomUUID();

            when(sessionExerciseRepository.findForUpdateWithWorkoutSession(sessionExerciseId, sessionId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> workoutSessionService.updateSessionExerciseNotes(
                            sessionId, sessionExerciseId, userId, new NotesUpdateRequest("new")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Session exercise not found");
        }
    }

    @Test
    @DisplayName("Should return page of workout session list items when getting all workout sessions")
    void should_returnPageOfWorkoutSessionListItems_when_gettingAllWorkoutSessions() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 2);
        WorkoutSession first = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                .withId(UUID.randomUUID())
                .forUser(UserBuilder.aUser(FAKER).build())
                .build();
        WorkoutSession second = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                .withId(UUID.randomUUID())
                .forUser(UserBuilder.aUser(FAKER).build())
                .build();
        Page<WorkoutSession> page = new PageImpl<>(List.of(first, second), pageable, 2);
        WorkoutSessionListItem firstDto = new WorkoutSessionListItem(
                first.getId(), SessionStatus.IN_PROGRESS, null, null, (RoutineListItem) null);
        WorkoutSessionListItem secondDto =
                new WorkoutSessionListItem(second.getId(), SessionStatus.COMPLETED, null, null, (RoutineListItem) null);

        when(workoutSessionRepository.findAllByUserId(userId, pageable)).thenReturn(page);
        when(workoutSessionMapper.toWorkoutSessionListItem(first)).thenReturn(firstDto);
        when(workoutSessionMapper.toWorkoutSessionListItem(second)).thenReturn(secondDto);

        Page<WorkoutSessionListItem> result = workoutSessionService.getAllWorkoutSessions(userId, pageable);

        assertThat(result.getContent()).containsExactly(firstDto, secondDto);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return workout session by id when session exists")
    void should_returnWorkoutSessionById_when_sessionExists() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                .withId(sessionId)
                .forUser(UserBuilder.aUser(FAKER).build())
                .build();
        WorkoutSessionResponse response = new WorkoutSessionResponse(
                sessionId, SessionStatus.IN_PROGRESS, null, null, "notes", List.of(), List.of());

        when(workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.of(session));
        when(workoutSessionMapper.toWorkoutSessionResponse(eq(session), anyList(), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponse result = workoutSessionService.getWorkoutSessionById(sessionId, userId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Should throw resource not found exception when workout session by id does not exist")
    void should_throwResourceNotFoundException_when_workoutSessionByIdDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutSessionService.getWorkoutSessionById(sessionId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Workout session not found");
    }

    @Test
    @DisplayName("Should create workout session from routine preserving exercises and default sets")
    void should_createWorkoutSessionFromRoutinePreservingExercisesAndDefaultSets() {
        UUID userId = UUID.randomUUID();
        UUID routineId = UUID.randomUUID();
        User userReference = UserBuilder.aUser(FAKER).withId(userId).build();
        Exercise firstExercise = ExerciseBuilder.anExercise(FAKER).build();
        firstExercise.setId(UUID.randomUUID());
        Exercise secondExercise =
                ExerciseBuilder.anExercise(FAKER).withCategory(Category.CARDIO).build();
        secondExercise.setId(UUID.randomUUID());
        Routine routine = RoutineBuilder.aRoutine(FAKER)
                .withId(routineId)
                .forUserId(userId)
                .withExercises(List.of())
                .build();
        RoutineExercise firstRoutineExercise = RoutineExerciseBuilder.aRoutineExercise(FAKER)
                .forRoutine(routine)
                .forExercise(firstExercise)
                .withPosition(1)
                .withDefaultSets(2)
                .withDefaultReps(10)
                .withDefaultWeightKg(20.0)
                .build();
        RoutineExercise secondRoutineExercise = RoutineExerciseBuilder.aRoutineExercise(FAKER)
                .forRoutine(routine)
                .forExercise(secondExercise)
                .withPosition(2)
                .withDefaultSets(1)
                .withDefaultReps(null)
                .withDefaultWeightKg(null)
                .withDefaultDurationSeconds(60)
                .withDefaultDistanceKm(1.2)
                .build();
        routine.setExercises(List.of(firstRoutineExercise, secondRoutineExercise));
        WorkoutSessionResponse response = new WorkoutSessionResponse(
                UUID.randomUUID(), SessionStatus.IN_PROGRESS, null, null, "created", List.of(), List.of());

        when(routineRepository.findByIdAndUserIdAndActiveTrue(routineId, userId))
                .thenReturn(Optional.of(routine));
        when(userRepository.getReferenceById(userId)).thenReturn(userReference);
        when(workoutSessionRepository.save(any(WorkoutSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutSessionMapper.toWorkoutSessionResponse(any(WorkoutSession.class), anyList(), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponse result = workoutSessionService.createWorkoutSessionFromRoutine(routineId, userId);

        ArgumentCaptor<WorkoutSession> savedSessionCaptor = ArgumentCaptor.forClass(WorkoutSession.class);
        verify(workoutSessionRepository).save(savedSessionCaptor.capture());
        WorkoutSession savedSession = savedSessionCaptor.getValue();

        assertThat(result).isEqualTo(response);
        assertThat(savedSession.getUser()).isEqualTo(userReference);
        assertThat(savedSession.getRoutine()).isEqualTo(routine);
        assertThat(savedSession.getExercises()).hasSize(2);
        assertThat(savedSession.getExercises())
                .extracting(SessionExercise::getPosition)
                .containsExactly(1, 2);
        assertThat(savedSession.getExercises().get(0).getSets()).hasSize(2);
        assertThat(savedSession.getExercises().get(0).getSets())
                .extracting(SessionSet::getSetNumber)
                .containsExactly(1, 2);
        assertThat(savedSession.getExercises().get(0).getSets())
                .extracting(SessionSet::getReps)
                .containsExactly(10, 10);
        assertThat(savedSession.getExercises().get(0).getSets())
                .extracting(SessionSet::getWeightKg)
                .containsExactly(20.0, 20.0);
        assertThat(savedSession.getExercises().get(1).getSets()).hasSize(1);
        assertThat(savedSession.getExercises().get(1).getSets().get(0).getDurationSeconds())
                .isEqualTo(60);
        assertThat(savedSession.getExercises().get(1).getSets().get(0).getDistanceKm())
                .isEqualTo(1.2);
    }

    @Test
    @DisplayName("Should throw resource not found exception when creating workout session from missing routine")
    void should_throwResourceNotFoundException_when_creatingWorkoutSessionFromMissingRoutine() {
        UUID userId = UUID.randomUUID();
        UUID routineId = UUID.randomUUID();

        when(routineRepository.findByIdAndUserIdAndActiveTrue(routineId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutSessionService.createWorkoutSessionFromRoutine(routineId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Routine not found");
        verify(workoutSessionRepository, never()).save(any(WorkoutSession.class));
    }

    @Test
    @DisplayName("Should reject completed session without completion time")
    void should_rejectCompletedSessionWithoutCompletionTime() {
        WorkoutSessionRequest request = new WorkoutSessionRequest(
                SessionStatus.COMPLETED,
                Instant.now().minusSeconds(60),
                null,
                null,
                List.of(new SessionExerciseRequest(
                        1, null, UUID.randomUUID(), List.of(new SessionSetRequest(1, 10, 20.0, null, null, true)))));

        assertThatThrownBy(() -> workoutSessionService.createWorkoutSessionFromScratch(request, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The completedAt field cannot be null for a completed workout session");
        verifyNoInteractions(exerciseQueryUseCase, workoutSessionRepository);
    }

    @Test
    @DisplayName("Should reject future completion time")
    void should_rejectFutureCompletionTime() {
        WorkoutSessionRequest request = new WorkoutSessionRequest(
                SessionStatus.COMPLETED,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(60),
                null,
                List.of());

        assertThatThrownBy(() -> workoutSessionService.createWorkoutSessionFromScratch(request, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("completedAt cannot be in the future");
        verifyNoInteractions(exerciseQueryUseCase, workoutSessionRepository);
    }

    @Test
    @DisplayName("Should reject completion time for in progress session")
    void should_rejectCompletionTimeForInProgressSession() {
        WorkoutSessionRequest request = new WorkoutSessionRequest(
                SessionStatus.IN_PROGRESS,
                Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(30),
                null,
                List.of());

        assertThatThrownBy(() -> workoutSessionService.createWorkoutSessionFromScratch(request, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The completedAt field can only be provided for a completed workout session");
        verifyNoInteractions(exerciseQueryUseCase, workoutSessionRepository);
    }

    @Test
    @DisplayName("Should create workout session from scratch with sequential positions")
    void should_createWorkoutSessionFromScratchWithSequentialPositions() {
        UUID userId = UUID.randomUUID();
        UUID firstExerciseId = UUID.randomUUID();
        UUID secondExerciseId = UUID.randomUUID();
        User userReference = UserBuilder.aUser(FAKER).withId(userId).build();
        Exercise firstExercise = ExerciseBuilder.anExercise(FAKER)
                .withCategory(Category.STRENGTH)
                .build();
        Exercise secondExercise =
                ExerciseBuilder.anExercise(FAKER).withCategory(Category.CARDIO).build();
        firstExercise.setId(firstExerciseId);
        secondExercise.setId(secondExerciseId);
        SessionExerciseRequest firstExerciseRequest = new SessionExerciseRequest(
                99, "first notes", firstExerciseId, List.of(new SessionSetRequest(99, 8, 40.0, null, null, true)));
        SessionExerciseRequest secondExerciseRequest = new SessionExerciseRequest(
                5,
                "second notes",
                secondExerciseId,
                List.of(new SessionSetRequest(null, null, null, 120, null, false)));
        WorkoutSessionRequest request = new WorkoutSessionRequest(
                SessionStatus.IN_PROGRESS,
                null,
                null,
                "session notes",
                List.of(firstExerciseRequest, secondExerciseRequest));
        WorkoutSessionResponse response = new WorkoutSessionResponse(
                UUID.randomUUID(), SessionStatus.IN_PROGRESS, null, null, "session notes", List.of(), List.of());

        when(exerciseQueryUseCase.findByIds(eq(List.of(firstExerciseId, secondExerciseId))))
                .thenReturn(Map.of(firstExerciseId, firstExercise, secondExerciseId, secondExercise));
        when(userRepository.getReferenceById(userId)).thenReturn(userReference);
        when(workoutSessionRepository.save(any(WorkoutSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutSessionMapper.toWorkoutSessionResponse(any(WorkoutSession.class), anyList(), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponse result = workoutSessionService.createWorkoutSessionFromScratch(request, userId);

        ArgumentCaptor<WorkoutSession> savedSessionCaptor = ArgumentCaptor.forClass(WorkoutSession.class);
        verify(workoutSessionRepository).save(savedSessionCaptor.capture());
        WorkoutSession savedSession = savedSessionCaptor.getValue();

        assertThat(result).isEqualTo(response);
        assertThat(savedSession.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(savedSession.getNotes()).isEqualTo("session notes");
        assertThat(savedSession.getCompletedAt()).isNull();
        assertThat(savedSession.getUser()).isEqualTo(userReference);
        assertThat(savedSession.getExercises()).hasSize(2);
        assertThat(savedSession.getExercises())
                .extracting(SessionExercise::getPosition)
                .containsExactly(1, 2);
        assertThat(savedSession.getExercises())
                .extracting(se -> se.getExercise().getId())
                .containsExactly(firstExerciseId, secondExerciseId);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getSetNumber())
                .isEqualTo(1);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getReps())
                .isEqualTo(8);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getWeightKg())
                .isEqualTo(40.0);
        assertThat(savedSession.getExercises().get(1).getSets().get(0).getSetNumber())
                .isEqualTo(1);
        assertThat(savedSession.getExercises().get(1).getSets().get(0).getDurationSeconds())
                .isEqualTo(120);
        assertThat(savedSession.getExercises().get(1).getSets().get(0).isCompleted())
                .isFalse();
    }

    @Test
    @DisplayName("Should synthesize a set from the last completed set when the request has no sets")
    void should_synthesizeSetFromLastCompletedSet_when_requestHasNoSets() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        User userReference = UserBuilder.aUser(FAKER).withId(userId).build();
        Exercise exercise = ExerciseBuilder.anExercise(FAKER)
                .withCategory(Category.STRENGTH)
                .build();
        exercise.setId(exerciseId);
        WorkoutSessionRequest request = new WorkoutSessionRequest(
                SessionStatus.IN_PROGRESS,
                Instant.parse("2025-01-01T10:00:00Z"),
                null,
                "session notes",
                List.of(new SessionExerciseRequest(null, "exercise notes", exerciseId, List.of())));
        WorkoutSessionResponse response = new WorkoutSessionResponse(
                UUID.randomUUID(), SessionStatus.IN_PROGRESS, null, null, "session notes", List.of(), List.of());
        LastSetProjection lastSet = mock(LastSetProjection.class);
        when(lastSet.getExerciseId()).thenReturn(exerciseId);
        when(lastSet.getReps()).thenReturn(15);
        when(lastSet.getWeightKg()).thenReturn(40.0);

        when(exerciseQueryUseCase.findByIds(eq(List.of(exerciseId)))).thenReturn(Map.of(exerciseId, exercise));
        when(sessionSetRepository.findLastSetsByExerciseIdAndUserId(Set.of(exerciseId), userId))
                .thenReturn(List.of(lastSet));
        when(userRepository.getReferenceById(userId)).thenReturn(userReference);
        when(workoutSessionRepository.save(any(WorkoutSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutSessionMapper.toWorkoutSessionResponse(any(WorkoutSession.class), anyList(), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponse result = workoutSessionService.createWorkoutSessionFromScratch(request, userId);

        ArgumentCaptor<WorkoutSession> savedSessionCaptor = ArgumentCaptor.forClass(WorkoutSession.class);
        verify(workoutSessionRepository).save(savedSessionCaptor.capture());
        WorkoutSession savedSession = savedSessionCaptor.getValue();

        assertThat(result).isEqualTo(response);
        assertThat(savedSession.getExercises()).hasSize(1);
        assertThat(savedSession.getExercises().get(0).getSets()).hasSize(1);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getSetNumber())
                .isEqualTo(1);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getReps())
                .isEqualTo(15);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getWeightKg())
                .isEqualTo(40.0);
        verifyNoInteractions(exerciseMetricsMapper);
    }

    @Test
    @DisplayName("Should synthesize a set from the category defaults when there is no last completed set")
    void should_synthesizeSetFromCategoryDefaults_when_noLastCompletedSetExists() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        User userReference = UserBuilder.aUser(FAKER).withId(userId).build();
        Exercise exercise = ExerciseBuilder.anExercise(FAKER)
                .withCategory(Category.STRENGTH)
                .build();
        exercise.setId(exerciseId);
        WorkoutSessionRequest request = new WorkoutSessionRequest(
                SessionStatus.IN_PROGRESS,
                Instant.parse("2025-01-01T10:00:00Z"),
                null,
                "session notes",
                List.of(new SessionExerciseRequest(null, "exercise notes", exerciseId, List.of())));
        WorkoutSessionResponse response = new WorkoutSessionResponse(
                UUID.randomUUID(), SessionStatus.IN_PROGRESS, null, null, "session notes", List.of(), List.of());

        when(exerciseQueryUseCase.findByIds(eq(List.of(exerciseId)))).thenReturn(Map.of(exerciseId, exercise));
        when(sessionSetRepository.findLastSetsByExerciseIdAndUserId(Set.of(exerciseId), userId))
                .thenReturn(List.of());
        when(exerciseMetricsMapper.toDefaultSessionSetRequest(Category.STRENGTH.defaultMetrics()))
                .thenReturn(new SessionSetRequest(1, 8, 5.0, null, null, false));
        when(userRepository.getReferenceById(userId)).thenReturn(userReference);
        when(workoutSessionRepository.save(any(WorkoutSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutSessionMapper.toWorkoutSessionResponse(any(WorkoutSession.class), anyList(), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponse result = workoutSessionService.createWorkoutSessionFromScratch(request, userId);

        ArgumentCaptor<WorkoutSession> savedSessionCaptor = ArgumentCaptor.forClass(WorkoutSession.class);
        verify(workoutSessionRepository).save(savedSessionCaptor.capture());
        WorkoutSession savedSession = savedSessionCaptor.getValue();

        assertThat(result).isEqualTo(response);
        assertThat(savedSession.getExercises().get(0).getSets()).hasSize(1);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getReps())
                .isEqualTo(8);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getWeightKg())
                .isEqualTo(5.0);
        verify(exerciseMetricsMapper).toDefaultSessionSetRequest(Category.STRENGTH.defaultMetrics());
    }

    @Test
    @DisplayName("Should reject a completed session without exercises")
    void should_rejectCompletedSessionWithoutExercises() {
        UUID userId = UUID.randomUUID();
        WorkoutSessionRequest request = new WorkoutSessionRequest(
                SessionStatus.COMPLETED,
                Instant.parse("2025-01-01T10:00:00Z"),
                Instant.parse("2025-01-01T11:00:00Z"),
                "notes",
                List.of());

        assertThatThrownBy(() -> workoutSessionService.createWorkoutSessionFromScratch(request, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A completed workout session must have at least one exercise");
        verifyNoInteractions(exerciseQueryUseCase, workoutSessionRepository);
    }

    @Test
    @DisplayName("Should create workout session from scratch when the exercises are not provided")
    void should_createWorkoutSessionFromScratch_when_exercisesAreNull() {
        UUID userId = UUID.randomUUID();
        User userReference = UserBuilder.aUser(FAKER).withId(userId).build();
        WorkoutSessionRequest request = new WorkoutSessionRequest(
                SessionStatus.IN_PROGRESS, Instant.parse("2025-01-01T10:00:00Z"), null, "session notes", null);
        WorkoutSessionResponse response = new WorkoutSessionResponse(
                UUID.randomUUID(), SessionStatus.IN_PROGRESS, null, null, "session notes", List.of(), List.of());

        when(userRepository.getReferenceById(userId)).thenReturn(userReference);
        when(workoutSessionRepository.save(any(WorkoutSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutSessionMapper.toWorkoutSessionResponse(any(WorkoutSession.class), anyList(), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponse result = workoutSessionService.createWorkoutSessionFromScratch(request, userId);

        ArgumentCaptor<WorkoutSession> savedSessionCaptor = ArgumentCaptor.forClass(WorkoutSession.class);
        verify(workoutSessionRepository).save(savedSessionCaptor.capture());
        WorkoutSession savedSession = savedSessionCaptor.getValue();

        assertThat(result).isEqualTo(response);
        assertThat(savedSession.getExercises()).isEmpty();
        verify(sessionSetRepository, never()).findLastSetsByExerciseIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("Should synthesize a set from the last completed set when the request sets are not provided")
    void should_synthesizeSetFromLastCompletedSet_when_requestSetsAreNull() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        User userReference = UserBuilder.aUser(FAKER).withId(userId).build();
        Exercise exercise = ExerciseBuilder.anExercise(FAKER)
                .withCategory(Category.STRENGTH)
                .build();
        exercise.setId(exerciseId);
        WorkoutSessionRequest request = new WorkoutSessionRequest(
                SessionStatus.IN_PROGRESS,
                Instant.parse("2025-01-01T10:00:00Z"),
                null,
                "session notes",
                List.of(new SessionExerciseRequest(null, "exercise notes", exerciseId, null)));
        WorkoutSessionResponse response = new WorkoutSessionResponse(
                UUID.randomUUID(), SessionStatus.IN_PROGRESS, null, null, "session notes", List.of(), List.of());
        LastSetProjection lastSet = mock(LastSetProjection.class);
        when(lastSet.getExerciseId()).thenReturn(exerciseId);
        when(lastSet.getReps()).thenReturn(15);
        when(lastSet.getWeightKg()).thenReturn(40.0);

        when(exerciseQueryUseCase.findByIds(eq(List.of(exerciseId)))).thenReturn(Map.of(exerciseId, exercise));
        when(sessionSetRepository.findLastSetsByExerciseIdAndUserId(Set.of(exerciseId), userId))
                .thenReturn(List.of(lastSet));
        when(userRepository.getReferenceById(userId)).thenReturn(userReference);
        when(workoutSessionRepository.save(any(WorkoutSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutSessionMapper.toWorkoutSessionResponse(any(WorkoutSession.class), anyList(), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponse result = workoutSessionService.createWorkoutSessionFromScratch(request, userId);

        ArgumentCaptor<WorkoutSession> savedSessionCaptor = ArgumentCaptor.forClass(WorkoutSession.class);
        verify(workoutSessionRepository).save(savedSessionCaptor.capture());
        WorkoutSession savedSession = savedSessionCaptor.getValue();

        assertThat(result).isEqualTo(response);
        assertThat(savedSession.getExercises().get(0).getSets()).hasSize(1);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getSetNumber())
                .isEqualTo(1);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getReps())
                .isEqualTo(15);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getWeightKg())
                .isEqualTo(40.0);
    }

    @Test
    @DisplayName("Should throw validation exception when creating workout session from scratch with exercise errors")
    void should_throwValidationException_when_creatingWorkoutSessionFromScratchWithExerciseErrors() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        WorkoutSessionRequest request = new WorkoutSessionRequest(
                SessionStatus.IN_PROGRESS,
                null,
                null,
                "notes",
                List.of(new SessionExerciseRequest(
                        null,
                        "exercise notes",
                        exerciseId,
                        List.of(new SessionSetRequest(1, 8, 20.0, null, null, true)))));

        when(exerciseQueryUseCase.findByIds(eq(List.of(exerciseId)))).thenReturn(Map.of());

        assertThatThrownBy(() -> workoutSessionService.createWorkoutSessionFromScratch(request, userId))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .containsEntry(exerciseId.toString(), List.of("Exercise does not exist")));
        verify(workoutSessionRepository, never()).save(any(WorkoutSession.class));
        verifyNoInteractions(workoutSessionMapper);
    }

    @Test
    @DisplayName("Should update notes when session is active")
    void should_updateNotes_when_sessionIsActive() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                .withId(sessionId)
                .forUser(UserBuilder.aUser(FAKER).build())
                .withNotes("old")
                .build();

        when(workoutSessionRepository.findForUpdateByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.of(session));

        workoutSessionService.updateWorkoutSessionNotes(sessionId, userId, new NotesUpdateRequest("new"));

        assertThat(session.getNotes()).isEqualTo("new");
    }

    @Test
    @DisplayName("Should throw bad request exception when updating notes for completed session")
    void should_throwBadRequestException_when_updatingNotesForCompletedSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                .withId(sessionId)
                .withStatus(SessionStatus.COMPLETED)
                .forUser(UserBuilder.aUser(FAKER).build())
                .build();

        when(workoutSessionRepository.findForUpdateByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> workoutSessionService.updateWorkoutSessionNotes(
                        sessionId, userId, new NotesUpdateRequest("new")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw resource not found exception when updating notes for missing session")
    void should_throwResourceNotFoundException_when_updatingNotesForMissingSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(workoutSessionRepository.findForUpdateByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutSessionService.updateWorkoutSessionNotes(
                        sessionId, userId, new NotesUpdateRequest("new")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Workout session not found");
    }

    @Test
    @DisplayName("Should complete session when session is active")
    void should_completeSession_when_sessionIsActive() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                .withId(sessionId)
                .forUser(UserBuilder.aUser(FAKER).build())
                .build();
        WorkoutSessionResponse response =
                new WorkoutSessionResponse(sessionId, SessionStatus.COMPLETED, null, null, null, List.of(), List.of());

        when(workoutSessionRepository.findForUpdateWithExercisesAndCatalogExercise(sessionId, userId))
                .thenReturn(Optional.of(session));
        when(workoutSessionMapper.toWorkoutSessionResponse(eq(session), anyList(), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponse result = workoutSessionService.completeWorkoutSession(sessionId, userId);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getCompletedAt()).isNotNull();
        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Should throw bad request exception when completing already completed session")
    void should_throwBadRequestException_when_completingAlreadyCompletedSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                .withId(sessionId)
                .withStatus(SessionStatus.COMPLETED)
                .forUser(UserBuilder.aUser(FAKER).build())
                .build();

        when(workoutSessionRepository.findForUpdateWithExercisesAndCatalogExercise(sessionId, userId))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> workoutSessionService.completeWorkoutSession(sessionId, userId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw resource not found exception when completing missing session")
    void should_throwResourceNotFoundException_when_completingMissingSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(workoutSessionRepository.findForUpdateWithExercisesAndCatalogExercise(sessionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutSessionService.completeWorkoutSession(sessionId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Workout session not found");
    }

    @Test
    @DisplayName("Should delete session when session exists")
    void should_deleteSession_when_sessionExists() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                .withId(sessionId)
                .forUser(UserBuilder.aUser(FAKER).build())
                .build();

        when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                .thenReturn(Optional.of(session));

        workoutSessionService.deleteWorkoutSession(sessionId, userId);

        verify(workoutSessionRepository).delete(session);
        verify(workoutSessionRepository).flush();
    }

    @Test
    @DisplayName("Should throw resource not found exception when deleting missing session")
    void should_throwResourceNotFoundException_when_deletingMissingSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutSessionService.deleteWorkoutSession(sessionId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Workout session not found");
    }

    @Nested
    @DisplayName("achievement evaluation")
    class AchievementEvaluation {

        @Test
        @DisplayName("Should evaluate the achievements and report them when the session is completed")
        void should_evaluateTheAchievements_when_theSessionIsCompleted() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .build();
            WorkoutSessionResponse response = new WorkoutSessionResponse(
                    sessionId, SessionStatus.COMPLETED, null, null, null, List.of(), List.of());

            when(workoutSessionRepository.findForUpdateWithExercisesAndCatalogExercise(sessionId, userId))
                    .thenReturn(Optional.of(session));
            when(workoutSessionMapper.toWorkoutSessionResponse(eq(session), anyList(), anyMap()))
                    .thenReturn(response);
            when(evaluateAchievementsUseCase.onSessionCompleted(userId, sessionId))
                    .thenReturn(List.of(unlockedAchievement("WORKOUTS_10")));

            workoutSessionService.completeWorkoutSession(sessionId, userId);

            verify(evaluateAchievementsUseCase).onSessionCompleted(userId, sessionId);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<UnlockedAchievement>> captor = ArgumentCaptor.forClass(List.class);
            verify(workoutSessionMapper).toWorkoutSessionResponse(eq(session), captor.capture(), anyMap());
            assertThat(captor.getValue()).extracting(UnlockedAchievement::code).containsExactly("WORKOUTS_10");
        }

        @Test
        @DisplayName("Should evaluate the achievements when a set is completed")
        void should_evaluateTheAchievements_when_theSetIsCompleted() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionExerciseId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();

            SessionSet sessionSet = SessionSetBuilder.aSessionSet(FAKER)
                    .withId(sessionSetId)
                    .withReps(10)
                    .withWeightKg(15.0)
                    .withCompleted(false)
                    .build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withSets(List.of(sessionSet))
                    .forExercise(ExerciseBuilder.anExercise(FAKER)
                            .withCategory(Category.STRENGTH)
                            .build())
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            sessionSet.setSessionExercise(sessionExercise);
            sessionExercise.setWorkoutSession(session);

            when(sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                            sessionSetId, sessionExerciseId, sessionId, userId))
                    .thenReturn(Optional.of(sessionSet));

            workoutSessionService.updateSessionSet(
                    sessionId,
                    sessionExerciseId,
                    sessionSetId,
                    userId,
                    new SessionSetRequest(1, 10, 15.0, null, null, true));

            verify(evaluateAchievementsUseCase).onSetCompleted(userId, sessionId, sessionSetId);
            verify(evaluateAchievementsUseCase, never()).refreshExerciseRecords(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("Should refresh the records when the metrics of a completed set change")
        void should_refreshTheRecords_when_theMetricsOfACompletedSetChange() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionExerciseId = UUID.randomUUID();
            UUID sessionSetId = UUID.randomUUID();

            Exercise exercise = ExerciseBuilder.anExercise(FAKER)
                    .withCategory(Category.STRENGTH)
                    .build();
            SessionSet sessionSet = SessionSetBuilder.aSessionSet(FAKER)
                    .withId(sessionSetId)
                    .withReps(10)
                    .withWeightKg(15.0)
                    .withCompleted(true)
                    .build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .withSets(List.of(sessionSet))
                    .forExercise(exercise)
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();
            sessionSet.setSessionExercise(sessionExercise);
            sessionExercise.setWorkoutSession(session);

            when(sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                            sessionSetId, sessionExerciseId, sessionId, userId))
                    .thenReturn(Optional.of(sessionSet));

            workoutSessionService.updateSessionSet(
                    sessionId,
                    sessionExerciseId,
                    sessionSetId,
                    userId,
                    new SessionSetRequest(1, 12, 20.0, null, null, true));

            verify(evaluateAchievementsUseCase).refreshExerciseRecords(userId, exercise.getId());
            verify(evaluateAchievementsUseCase, never())
                    .onSetCompleted(any(UUID.class), any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("Should refresh the records of every exercise when the session is deleted")
        void should_refreshTheRecordsOfEveryExercise_when_theSessionIsDeleted() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();

            Exercise exercise = ExerciseBuilder.anExercise(FAKER)
                    .withCategory(Category.STRENGTH)
                    .build();
            SessionExercise sessionExercise = SessionExerciseBuilder.aSessionExercise(FAKER)
                    .forExercise(exercise)
                    .build();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .withExercises(List.of(sessionExercise))
                    .build();

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));

            workoutSessionService.deleteWorkoutSession(sessionId, userId);

            verify(evaluateAchievementsUseCase).refreshExerciseRecords(userId, exercise.getId());
        }

        private UnlockedAchievementResult unlockedAchievement(String code) {
            return new UnlockedAchievementResult(
                    UUID.randomUUID(), code, code, AchievementTier.BRONZE, null, Instant.now());
        }
    }
}
