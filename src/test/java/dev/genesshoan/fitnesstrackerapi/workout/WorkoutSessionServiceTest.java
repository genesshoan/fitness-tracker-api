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

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ValidationException;
import dev.genesshoan.fitnesstrackerapi.common.mapper.ExerciseMetricsMapper;
import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ExerciseFinder;
import dev.genesshoan.fitnesstrackerapi.routine.RoutineRepository;
import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.routine.domain.RoutineExercise;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineListItemDTO;
import dev.genesshoan.fitnesstrackerapi.stats.service.StatsService;
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
    private ExerciseRepository exerciseRepository;

    @Mock
    private ExerciseFinder exerciseFinder;

    @Mock
    private WorkoutSessionMapper workoutSessionMapper;

    @Mock
    private SessionExerciseMapper sessionExerciseMapper;

    @Mock
    private SessionSetMapper sessionSetMapper;

    @Mock
    private ExerciseMetricsMapper exerciseMetricsMapper;

    @Mock
    private StatsService statsService;

    @InjectMocks
    private WorkoutSessionService workoutSessionService;

    @BeforeEach
    void configureStatsService() {
        lenient()
                .when(statsService.calculateForSession(any(WorkoutSession.class), any(UUID.class)))
                .thenReturn(Map.of());
        lenient()
                .when(statsService.calculateForSet(any(SessionSet.class), any(UUID.class)))
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
            SessionExerciseRequestDTO dto = new SessionExerciseRequestDTO(
                    2, "notes", exerciseId, List.of(new SessionSetRequestDTO(1, 10, 20.0, null, null, true)));
            SessionExerciseAddedResponseDTO response = new SessionExerciseAddedResponseDTO(
                    null,
                    List.of(
                            new SessionExercisePositionDTO(first.getId(), 1),
                            new SessionExercisePositionDTO(second.getId(), 3)));

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));
            when(exerciseRepository.findByIdAndActiveTrue(exerciseId)).thenReturn(Optional.of(exercise));
            when(sessionExerciseMapper.toSessionExerciseAddedResponseDTO(any(SessionExercise.class), any()))
                    .thenReturn(response);

            SessionExerciseAddedResponseDTO result =
                    workoutSessionService.addNewSessionExercise(sessionId, userId, dto);

            ArgumentCaptor<SessionExercise> addedExerciseCaptor = ArgumentCaptor.forClass(SessionExercise.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SessionExercise>> shiftedCaptor = ArgumentCaptor.forClass(List.class);

            verify(sessionExerciseMapper)
                    .toSessionExerciseAddedResponseDTO(addedExerciseCaptor.capture(), shiftedCaptor.capture());

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
            verify(exerciseRepository).findByIdAndActiveTrue(exerciseId);
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
                            new SessionExerciseRequestDTO(
                                    1,
                                    "notes",
                                    UUID.randomUUID(),
                                    List.of(new SessionSetRequestDTO(1, 10, 20.0, null, null, true)))))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The workout session is already finished");

            verify(workoutSessionRepository).findForUpdateWithExercises(sessionId, userId);
            verifyNoInteractions(exerciseRepository, sessionExerciseMapper, sessionSetMapper, sessionSetRepository);
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
                            new SessionExerciseRequestDTO(
                                    1,
                                    "notes",
                                    UUID.randomUUID(),
                                    List.of(new SessionSetRequestDTO(1, 10, 20.0, null, null, true)))))
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
            SessionExerciseRequestDTO dto = new SessionExerciseRequestDTO(
                    1, "notes", exerciseId, List.of(new SessionSetRequestDTO(1, null, null, 5, null, true)));

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));
            when(exerciseRepository.findByIdAndActiveTrue(exerciseId)).thenReturn(Optional.of(exercise));

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
            when(exerciseRepository.findByIdAndActiveTrue(exerciseId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workoutSessionService.addNewSessionExercise(
                            sessionId,
                            userId,
                            new SessionExerciseRequestDTO(
                                    1,
                                    "notes",
                                    exerciseId,
                                    List.of(new SessionSetRequestDTO(1, 10, 20.0, null, null, true)))))
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
            SessionExerciseRequestDTO dto = new SessionExerciseRequestDTO(
                    null, "notes", exerciseId, List.of(new SessionSetRequestDTO(1, 10, 20.0, null, null, true)));

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));
            when(exerciseRepository.findByIdAndActiveTrue(exerciseId)).thenReturn(Optional.of(exercise));

            workoutSessionService.addNewSessionExercise(sessionId, userId, dto);

            ArgumentCaptor<SessionExercise> addedExerciseCaptor = ArgumentCaptor.forClass(SessionExercise.class);
            verify(sessionExerciseMapper).toSessionExerciseAddedResponseDTO(addedExerciseCaptor.capture(), any());

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
            List<SessionExercisePositionDTO> response = List.of(
                    new SessionExercisePositionDTO(second.getId(), 1),
                    new SessionExercisePositionDTO(first.getId(), 2),
                    new SessionExercisePositionDTO(third.getId(), 3));
            when(sessionExerciseMapper.toSessionExercisePositionDTOList(any())).thenReturn(response);

            List<SessionExercisePositionDTO> result = workoutSessionService.updateSessionExercisePosition(
                    sessionId, second.getId(), userId, new PositionRequestDTO(1));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SessionExercise>> mapperInputCaptor = ArgumentCaptor.forClass(List.class);
            verify(sessionExerciseMapper).toSessionExercisePositionDTOList(mapperInputCaptor.capture());

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
            List<SessionExercisePositionDTO> response = List.of(
                    new SessionExercisePositionDTO(second.getId(), 1),
                    new SessionExercisePositionDTO(third.getId(), 2),
                    new SessionExercisePositionDTO(first.getId(), 3));
            when(sessionExerciseMapper.toSessionExercisePositionDTOList(any())).thenReturn(response);

            List<SessionExercisePositionDTO> result = workoutSessionService.updateSessionExercisePosition(
                    sessionId, first.getId(), userId, new PositionRequestDTO(3));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SessionExercise>> mapperInputCaptor = ArgumentCaptor.forClass(List.class);
            verify(sessionExerciseMapper).toSessionExercisePositionDTOList(mapperInputCaptor.capture());

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
            List<SessionExercisePositionDTO> response = List.of(
                    new SessionExercisePositionDTO(first.getId(), 1),
                    new SessionExercisePositionDTO(second.getId(), 2));

            when(workoutSessionRepository.findForUpdateWithExercises(sessionId, userId))
                    .thenReturn(Optional.of(session));
            when(sessionExerciseMapper.toSessionExercisePositionDTOList(any())).thenReturn(response);

            List<SessionExercisePositionDTO> result = workoutSessionService.updateSessionExercisePosition(
                    sessionId, second.getId(), userId, new PositionRequestDTO(2));

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
                            sessionId, first.getId(), userId, new PositionRequestDTO(1)))
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
                            sessionId, UUID.randomUUID(), userId, new PositionRequestDTO(1)))
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
            List<SessionExercisePositionDTO> response = List.of(
                    new SessionExercisePositionDTO(first.getId(), 1), new SessionExercisePositionDTO(third.getId(), 2));
            when(sessionExerciseMapper.toSessionExercisePositionDTOList(any())).thenReturn(response);

            List<SessionExercisePositionDTO> result =
                    workoutSessionService.deleteSessionExercise(sessionId, second.getId(), userId);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SessionExercise>> mapperInputCaptor = ArgumentCaptor.forClass(List.class);
            verify(sessionExerciseMapper).toSessionExercisePositionDTOList(mapperInputCaptor.capture());

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
            SessionSetRequestDTO dto = new SessionSetRequestDTO(null, 8, 20.0, null, null, true);

            when(sessionExerciseRepository.findForUpdateWithWorkoutSessionAndExerciseAndSets(
                            sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));
            when(sessionSetRepository.save(any(SessionSet.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(sessionSetMapper.toResponseDTO(any(SessionSet.class), anyMap()))
                    .thenAnswer(invocation -> {
                        SessionSet set = invocation.getArgument(0);
                        return new SessionSetResponseDTO(
                                set.getId(),
                                set.getSetNumber(),
                                set.getReps(),
                                set.getWeightKg(),
                                set.getDurationSeconds(),
                                set.getDistanceKm(),
                                set.isCompleted(),
                                List.of());
                    });

            SessionSetResponseDTO result =
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
                            new SessionSetRequestDTO(null, 8, 20.0, null, null, true)))
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
            SessionSetRequestDTO dto = new SessionSetRequestDTO(null, null, null, null, null, false);

            when(sessionExerciseRepository.findForUpdateWithWorkoutSessionAndExerciseAndSets(
                            sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));
            when(sessionSetMapper.toDefaultSessionSetRequestDTO(lastSet))
                    .thenReturn(new SessionSetRequestDTO(1, 12, 30.0, null, null, false));
            when(sessionSetRepository.save(any(SessionSet.class))).thenAnswer(invocation -> invocation.getArgument(0));

            workoutSessionService.addNewSessionSet(sessionId, sessionExercise.getId(), userId, dto);

            ArgumentCaptor<SessionSet> savedSetCaptor = ArgumentCaptor.forClass(SessionSet.class);
            verify(sessionSetRepository).save(savedSetCaptor.capture());

            SessionSet savedSet = savedSetCaptor.getValue();
            assertThat(savedSet.getSetNumber()).isEqualTo(2);
            assertThat(savedSet.getReps()).isEqualTo(12);
            assertThat(savedSet.getWeightKg()).isEqualTo(30.0);
            assertThat(savedSet.isCompleted()).isFalse();
            verify(sessionSetMapper).toDefaultSessionSetRequestDTO(lastSet);
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
            SessionSetRequestDTO dto = new SessionSetRequestDTO(null, null, null, null, null, false);

            when(sessionExerciseRepository.findForUpdateWithWorkoutSessionAndExerciseAndSets(
                            sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));
            when(exerciseMetricsMapper.toDefaultSessionSetRequestDTO(Category.STRENGTH.defaultMetrics()))
                    .thenReturn(new SessionSetRequestDTO(1, 8, 5.0, null, null, false));
            when(sessionSetRepository.save(any(SessionSet.class))).thenAnswer(invocation -> invocation.getArgument(0));

            workoutSessionService.addNewSessionSet(sessionId, sessionExercise.getId(), userId, dto);

            ArgumentCaptor<SessionSet> savedSetCaptor = ArgumentCaptor.forClass(SessionSet.class);
            verify(sessionSetRepository).save(savedSetCaptor.capture());

            SessionSet savedSet = savedSetCaptor.getValue();
            assertThat(savedSet.getSetNumber()).isEqualTo(1);
            assertThat(savedSet.getReps()).isEqualTo(8);
            assertThat(savedSet.getWeightKg()).isEqualTo(5.0);
            verify(exerciseMetricsMapper).toDefaultSessionSetRequestDTO(Category.STRENGTH.defaultMetrics());
            verify(sessionSetMapper, never()).toDefaultSessionSetRequestDTO(any(SessionSet.class));
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
            SessionSetRequestDTO dto = new SessionSetRequestDTO(1, 12, 20.0, null, null, true);

            when(sessionSetRepository.findForUpdateWithSessionExerciseAndWorkoutSessionAndExercise(
                            sessionSetId, sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionSet));
            when(sessionSetMapper.toResponseDTO(eq(sessionSet), anyMap())).thenAnswer(invocation -> {
                SessionSet set = invocation.getArgument(0);
                return new SessionSetResponseDTO(
                        set.getId(),
                        set.getSetNumber(),
                        set.getReps(),
                        set.getWeightKg(),
                        set.getDurationSeconds(),
                        set.getDistanceKm(),
                        set.isCompleted(),
                        List.of());
            });

            SessionSetResponseDTO result = workoutSessionService.updateSessionSet(
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
                            new SessionSetRequestDTO(1, 12, 20.0, null, null, true)))
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
                            new SessionSetRequestDTO(1, null, null, 5, null, true)))
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
                            new SessionSetRequestDTO(1, 12, 20.0, null, null, true)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Session set not found");
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
                    sessionId, sessionExerciseId, userId, new NotesUpdateRequestDTO("new"));

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
                            sessionId, sessionExerciseId, userId, new NotesUpdateRequestDTO("new")))
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
                            sessionId, sessionExerciseId, userId, new NotesUpdateRequestDTO("new")))
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
        WorkoutSessionListItemDTO firstDto = new WorkoutSessionListItemDTO(
                first.getId(), SessionStatus.IN_PROGRESS, null, null, (RoutineListItemDTO) null);
        WorkoutSessionListItemDTO secondDto = new WorkoutSessionListItemDTO(
                second.getId(), SessionStatus.COMPLETED, null, null, (RoutineListItemDTO) null);

        when(workoutSessionRepository.findAllByUserId(userId, pageable)).thenReturn(page);
        when(workoutSessionMapper.toWorkoutSessionListItemDTO(first)).thenReturn(firstDto);
        when(workoutSessionMapper.toWorkoutSessionListItemDTO(second)).thenReturn(secondDto);

        Page<WorkoutSessionListItemDTO> result = workoutSessionService.getAllWorkoutSessions(userId, pageable);

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
        WorkoutSessionResponseDTO response =
                new WorkoutSessionResponseDTO(sessionId, SessionStatus.IN_PROGRESS, null, null, "notes", List.of());

        when(workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.of(session));
        when(workoutSessionMapper.toWorkoutSessionResponseDTO(eq(session), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponseDTO result = workoutSessionService.getWorkoutSessionById(sessionId, userId);

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
        WorkoutSessionResponseDTO response = new WorkoutSessionResponseDTO(
                UUID.randomUUID(), SessionStatus.IN_PROGRESS, null, null, "created", List.of());

        when(routineRepository.findByIdAndUserIdAndActiveTrue(routineId, userId))
                .thenReturn(Optional.of(routine));
        when(userRepository.getReferenceById(userId)).thenReturn(userReference);
        when(workoutSessionRepository.save(any(WorkoutSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutSessionMapper.toWorkoutSessionResponseDTO(any(WorkoutSession.class), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponseDTO result = workoutSessionService.createWorkoutSessionFromRoutine(routineId, userId);

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
        WorkoutSessionRequestDTO request = new WorkoutSessionRequestDTO(
                SessionStatus.COMPLETED,
                Instant.now().minusSeconds(60),
                null,
                null,
                List.of(new SessionExerciseRequestDTO(
                        1, null, UUID.randomUUID(), List.of(new SessionSetRequestDTO(1, 10, 20.0, null, null, true)))));

        assertThatThrownBy(() -> workoutSessionService.createWorkoutSessionFromScratch(request, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The completedAt field cannot be null for a completed workout session");
        verifyNoInteractions(exerciseFinder, workoutSessionRepository);
    }

    @Test
    @DisplayName("Should reject future completion time")
    void should_rejectFutureCompletionTime() {
        WorkoutSessionRequestDTO request = new WorkoutSessionRequestDTO(
                SessionStatus.COMPLETED,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(60),
                null,
                List.of());

        assertThatThrownBy(() -> workoutSessionService.createWorkoutSessionFromScratch(request, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("completedAt cannot be in the future");
        verifyNoInteractions(exerciseFinder, workoutSessionRepository);
    }

    @Test
    @DisplayName("Should reject completion time for in progress session")
    void should_rejectCompletionTimeForInProgressSession() {
        WorkoutSessionRequestDTO request = new WorkoutSessionRequestDTO(
                SessionStatus.IN_PROGRESS,
                Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(30),
                null,
                List.of());

        assertThatThrownBy(() -> workoutSessionService.createWorkoutSessionFromScratch(request, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The completedAt field can only be provided for a completed workout session");
        verifyNoInteractions(exerciseFinder, workoutSessionRepository);
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
        SessionExerciseRequestDTO firstExerciseRequest = new SessionExerciseRequestDTO(
                99, "first notes", firstExerciseId, List.of(new SessionSetRequestDTO(99, 8, 40.0, null, null, true)));
        SessionExerciseRequestDTO secondExerciseRequest = new SessionExerciseRequestDTO(
                5,
                "second notes",
                secondExerciseId,
                List.of(new SessionSetRequestDTO(null, null, null, 120, null, false)));
        WorkoutSessionRequestDTO request = new WorkoutSessionRequestDTO(
                SessionStatus.IN_PROGRESS,
                null,
                null,
                "session notes",
                List.of(firstExerciseRequest, secondExerciseRequest));
        WorkoutSessionResponseDTO response = new WorkoutSessionResponseDTO(
                UUID.randomUUID(), SessionStatus.IN_PROGRESS, null, null, "session notes", List.of());

        when(exerciseFinder.findActiveByIds(eq(List.of(firstExerciseId, secondExerciseId)), any()))
                .thenReturn(Map.of(firstExerciseId, firstExercise, secondExerciseId, secondExercise));
        when(userRepository.getReferenceById(userId)).thenReturn(userReference);
        when(workoutSessionRepository.save(any(WorkoutSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutSessionMapper.toWorkoutSessionResponseDTO(any(WorkoutSession.class), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponseDTO result = workoutSessionService.createWorkoutSessionFromScratch(request, userId);

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
        WorkoutSessionRequestDTO request = new WorkoutSessionRequestDTO(
                SessionStatus.IN_PROGRESS,
                Instant.parse("2025-01-01T10:00:00Z"),
                null,
                "session notes",
                List.of(new SessionExerciseRequestDTO(null, "exercise notes", exerciseId, List.of())));
        WorkoutSessionResponseDTO response = new WorkoutSessionResponseDTO(
                UUID.randomUUID(), SessionStatus.IN_PROGRESS, null, null, "session notes", List.of());
        LastSetProjection lastSet = mock(LastSetProjection.class);
        when(lastSet.getExerciseId()).thenReturn(exerciseId);
        when(lastSet.getReps()).thenReturn(15);
        when(lastSet.getWeightKg()).thenReturn(40.0);

        when(exerciseFinder.findActiveByIds(eq(List.of(exerciseId)), any())).thenReturn(Map.of(exerciseId, exercise));
        when(sessionSetRepository.findLastSetsByExerciseIdAndUserId(Set.of(exerciseId), userId))
                .thenReturn(List.of(lastSet));
        when(userRepository.getReferenceById(userId)).thenReturn(userReference);
        when(workoutSessionRepository.save(any(WorkoutSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutSessionMapper.toWorkoutSessionResponseDTO(any(WorkoutSession.class), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponseDTO result = workoutSessionService.createWorkoutSessionFromScratch(request, userId);

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
        WorkoutSessionRequestDTO request = new WorkoutSessionRequestDTO(
                SessionStatus.IN_PROGRESS,
                Instant.parse("2025-01-01T10:00:00Z"),
                null,
                "session notes",
                List.of(new SessionExerciseRequestDTO(null, "exercise notes", exerciseId, List.of())));
        WorkoutSessionResponseDTO response = new WorkoutSessionResponseDTO(
                UUID.randomUUID(), SessionStatus.IN_PROGRESS, null, null, "session notes", List.of());

        when(exerciseFinder.findActiveByIds(eq(List.of(exerciseId)), any())).thenReturn(Map.of(exerciseId, exercise));
        when(sessionSetRepository.findLastSetsByExerciseIdAndUserId(Set.of(exerciseId), userId))
                .thenReturn(List.of());
        when(exerciseMetricsMapper.toDefaultSessionSetRequestDTO(Category.STRENGTH.defaultMetrics()))
                .thenReturn(new SessionSetRequestDTO(1, 8, 5.0, null, null, false));
        when(userRepository.getReferenceById(userId)).thenReturn(userReference);
        when(workoutSessionRepository.save(any(WorkoutSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutSessionMapper.toWorkoutSessionResponseDTO(any(WorkoutSession.class), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponseDTO result = workoutSessionService.createWorkoutSessionFromScratch(request, userId);

        ArgumentCaptor<WorkoutSession> savedSessionCaptor = ArgumentCaptor.forClass(WorkoutSession.class);
        verify(workoutSessionRepository).save(savedSessionCaptor.capture());
        WorkoutSession savedSession = savedSessionCaptor.getValue();

        assertThat(result).isEqualTo(response);
        assertThat(savedSession.getExercises().get(0).getSets()).hasSize(1);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getReps())
                .isEqualTo(8);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getWeightKg())
                .isEqualTo(5.0);
        verify(exerciseMetricsMapper).toDefaultSessionSetRequestDTO(Category.STRENGTH.defaultMetrics());
    }

    @Test
    @DisplayName("Should reject a completed session without exercises")
    void should_rejectCompletedSessionWithoutExercises() {
        UUID userId = UUID.randomUUID();
        WorkoutSessionRequestDTO request = new WorkoutSessionRequestDTO(
                SessionStatus.COMPLETED,
                Instant.parse("2025-01-01T10:00:00Z"),
                Instant.parse("2025-01-01T11:00:00Z"),
                "notes",
                List.of());

        assertThatThrownBy(() -> workoutSessionService.createWorkoutSessionFromScratch(request, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A completed workout session must have at least one exercise");
        verifyNoInteractions(exerciseFinder, workoutSessionRepository);
    }

    @Test
    @DisplayName("Should create workout session from scratch when the exercises are not provided")
    void should_createWorkoutSessionFromScratch_when_exercisesAreNull() {
        UUID userId = UUID.randomUUID();
        User userReference = UserBuilder.aUser(FAKER).withId(userId).build();
        WorkoutSessionRequestDTO request = new WorkoutSessionRequestDTO(
                SessionStatus.IN_PROGRESS, Instant.parse("2025-01-01T10:00:00Z"), null, "session notes", null);
        WorkoutSessionResponseDTO response = new WorkoutSessionResponseDTO(
                UUID.randomUUID(), SessionStatus.IN_PROGRESS, null, null, "session notes", List.of());

        when(exerciseFinder.findActiveByIds(eq(List.of()), any())).thenReturn(Map.of());
        when(userRepository.getReferenceById(userId)).thenReturn(userReference);
        when(workoutSessionRepository.save(any(WorkoutSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutSessionMapper.toWorkoutSessionResponseDTO(any(WorkoutSession.class), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponseDTO result = workoutSessionService.createWorkoutSessionFromScratch(request, userId);

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
        WorkoutSessionRequestDTO request = new WorkoutSessionRequestDTO(
                SessionStatus.IN_PROGRESS,
                Instant.parse("2025-01-01T10:00:00Z"),
                null,
                "session notes",
                List.of(new SessionExerciseRequestDTO(null, "exercise notes", exerciseId, null)));
        WorkoutSessionResponseDTO response = new WorkoutSessionResponseDTO(
                UUID.randomUUID(), SessionStatus.IN_PROGRESS, null, null, "session notes", List.of());
        LastSetProjection lastSet = mock(LastSetProjection.class);
        when(lastSet.getExerciseId()).thenReturn(exerciseId);
        when(lastSet.getReps()).thenReturn(15);
        when(lastSet.getWeightKg()).thenReturn(40.0);

        when(exerciseFinder.findActiveByIds(eq(List.of(exerciseId)), any())).thenReturn(Map.of(exerciseId, exercise));
        when(sessionSetRepository.findLastSetsByExerciseIdAndUserId(Set.of(exerciseId), userId))
                .thenReturn(List.of(lastSet));
        when(userRepository.getReferenceById(userId)).thenReturn(userReference);
        when(workoutSessionRepository.save(any(WorkoutSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutSessionMapper.toWorkoutSessionResponseDTO(any(WorkoutSession.class), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponseDTO result = workoutSessionService.createWorkoutSessionFromScratch(request, userId);

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
        WorkoutSessionRequestDTO request = new WorkoutSessionRequestDTO(
                SessionStatus.IN_PROGRESS,
                null,
                null,
                "notes",
                List.of(new SessionExerciseRequestDTO(
                        null,
                        "exercise notes",
                        exerciseId,
                        List.of(new SessionSetRequestDTO(1, 8, 20.0, null, null, true)))));

        when(exerciseFinder.findActiveByIds(eq(List.of(exerciseId)), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<String, List<String>> errors = invocation.getArgument(1);
            errors.put(exerciseId.toString(), List.of("Exercise does not exist"));
            return Map.of();
        });

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

        workoutSessionService.updateWorkoutSessionNotes(sessionId, userId, new NotesUpdateRequestDTO("new"));

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
                        sessionId, userId, new NotesUpdateRequestDTO("new")))
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
                        sessionId, userId, new NotesUpdateRequestDTO("new")))
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
        WorkoutSessionResponseDTO response =
                new WorkoutSessionResponseDTO(sessionId, SessionStatus.COMPLETED, null, null, null, List.of());

        when(workoutSessionRepository.findForUpdateWithExercisesAndCatalogExercise(sessionId, userId))
                .thenReturn(Optional.of(session));
        when(workoutSessionMapper.toWorkoutSessionResponseDTO(eq(session), anyMap()))
                .thenReturn(response);

        WorkoutSessionResponseDTO result = workoutSessionService.completeWorkoutSession(sessionId, userId);

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

        when(workoutSessionRepository.findForUpdateByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.of(session));

        workoutSessionService.deleteWorkoutSession(sessionId, userId);

        verify(workoutSessionRepository).delete(session);
    }

    @Test
    @DisplayName("Should throw resource not found exception when deleting missing session")
    void should_throwResourceNotFoundException_when_deletingMissingSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(workoutSessionRepository.findForUpdateByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutSessionService.deleteWorkoutSession(sessionId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Workout session not found");
    }
}
