package dev.genesshoan.fitnesstrackerapi.unit;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ValidationException;
import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ExerciseFinder;
import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.routine.domain.RoutineExercise;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineListItemDTO;
import dev.genesshoan.fitnesstrackerapi.routine.RoutineRepository;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.RoutineBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.RoutineExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.SessionSetBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.UserBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.WorkoutSessionBuilder;
import dev.genesshoan.fitnesstrackerapi.user.UserRepository;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.workout.WorkoutSessionService;
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
import net.datafaker.Faker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @InjectMocks
    private WorkoutSessionService workoutSessionService;

    @Nested
    @DisplayName("addNewSessionExercise")
    class AddNewSessionExercise {
        @Test
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

            when(workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId))
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
            assertThat(shiftedExercises).extracting(SessionExercise::getId).containsExactly(first.getId(), second.getId());
            assertThat(shiftedExercises).extracting(SessionExercise::getPosition).containsExactly(1, 3);
            verify(workoutSessionRepository).findWithExercisesByIdAndUserId(sessionId, userId);
            verify(exerciseRepository).findByIdAndActiveTrue(exerciseId);
        }

        @Test
        void should_throwBadRequestException_when_sessionIsCompleted() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .withStatus(SessionStatus.COMPLETED)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .build();

            when(workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId))
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

            verify(workoutSessionRepository).findWithExercisesByIdAndUserId(sessionId, userId);
            verifyNoInteractions(exerciseRepository, sessionExerciseMapper, sessionSetMapper, sessionSetRepository);
        }

        @Test
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

            when(workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId))
                    .thenReturn(Optional.of(session));
            when(exerciseRepository.findByIdAndActiveTrue(exerciseId)).thenReturn(Optional.of(exercise));

            assertThatThrownBy(() -> workoutSessionService.addNewSessionExercise(sessionId, userId, dto))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                            .containsEntry(
                                    exerciseId.toString(),
                                    List.of("Invalid data for set 1 for category: STRENGTH")));
            verifyNoInteractions(sessionExerciseMapper, sessionSetRepository);
        }

        @Test
        void should_throwResourceNotFoundException_when_exerciseDoesNotExist() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID exerciseId = UUID.randomUUID();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .build();

            when(workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId))
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
    }

    @Nested
    @DisplayName("updateSessionExercisePosition")
    class UpdateSessionExercisePosition {
        @Test
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

            when(workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId))
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
            assertThat(mapperInputCaptor.getValue()).extracting(SessionExercise::getId)
                    .containsExactly(second.getId(), first.getId(), third.getId());
        }

        @Test
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

            when(workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId))
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
            assertThat(mapperInputCaptor.getValue()).extracting(SessionExercise::getId)
                    .containsExactly(second.getId(), third.getId(), first.getId());
        }

        @Test
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

            when(workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId))
                    .thenReturn(Optional.of(session));
            when(sessionExerciseMapper.toSessionExercisePositionDTOList(any())).thenReturn(response);

            List<SessionExercisePositionDTO> result = workoutSessionService.updateSessionExercisePosition(
                    sessionId, second.getId(), userId, new PositionRequestDTO(2));

            assertThat(result).isEqualTo(response);
            assertThat(first.getPosition()).isEqualTo(1);
            assertThat(second.getPosition()).isEqualTo(2);
        }

        @Test
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

            when(workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId))
                    .thenReturn(Optional.of(session));

            assertThatThrownBy(() -> workoutSessionService.updateSessionExercisePosition(
                            sessionId, first.getId(), userId, new PositionRequestDTO(1)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The workout session is already finished");
        }

        @Test
        void should_throwResourceNotFoundException_when_sessionExerciseDoesNotExist() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .build();

            when(workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId))
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

            when(workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId))
                    .thenReturn(Optional.of(session));
            List<SessionExercisePositionDTO> response = List.of(
                    new SessionExercisePositionDTO(first.getId(), 1),
                    new SessionExercisePositionDTO(third.getId(), 2));
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
            assertThat(mapperInputCaptor.getValue()).extracting(SessionExercise::getId).containsExactly(first.getId(), third.getId());
        }

        @Test
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

            when(workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId))
                    .thenReturn(Optional.of(session));

            assertThatThrownBy(() -> workoutSessionService.deleteSessionExercise(sessionId, first.getId(), userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The workout session is already finished");
        }

        @Test
        void should_throwResourceNotFoundException_when_deletingExerciseThatDoesNotExistInSession() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                    .withId(sessionId)
                    .forUser(UserBuilder.aUser(FAKER).build())
                    .build();

            when(workoutSessionRepository.findWithExercisesByIdAndUserId(sessionId, userId))
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

            when(sessionExerciseRepository.findWithSetsByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
                            sessionExercise.getId(), sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));
            when(sessionSetRepository.save(any(SessionSet.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(sessionSetMapper.toSessionSetResponseDTO(any(SessionSet.class)))
                    .thenAnswer(invocation -> {
                        SessionSet set = invocation.getArgument(0);
                        return new SessionSetResponseDTO(
                                set.getId(),
                                set.getSetNumber(),
                                set.getReps(),
                                set.getWeightKg(),
                                set.getDurationSeconds(),
                                set.getDistanceKm(),
                                set.isCompleted());
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

            when(sessionExerciseRepository.findWithSetsByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
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
    }

    @Nested
    @DisplayName("updateSessionExerciseNotes")
    class UpdateSessionExerciseNotes {
        @Test
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

            when(sessionExerciseRepository.findWithWorkoutSessionByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
                            sessionExerciseId, sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));

            workoutSessionService.updateSessionExerciseNotes(
                    sessionId, sessionExerciseId, userId, new NotesUpdateRequestDTO("new"));

            assertThat(sessionExercise.getNotes()).isEqualTo("new");
        }

        @Test
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

            when(sessionExerciseRepository.findWithWorkoutSessionByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
                            sessionExerciseId, sessionId, userId))
                    .thenReturn(Optional.of(sessionExercise));

            assertThatThrownBy(() -> workoutSessionService.updateSessionExerciseNotes(
                            sessionId, sessionExerciseId, userId, new NotesUpdateRequestDTO("new")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The workout session is already finished");
        }

        @Test
        void should_throwResourceNotFoundException_when_updatingNotesForMissingSessionExercise() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionExerciseId = UUID.randomUUID();

            when(sessionExerciseRepository.findWithWorkoutSessionByIdAndWorkoutSessionIdAndWorkoutSessionUserId(
                            sessionExerciseId, sessionId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> workoutSessionService.updateSessionExerciseNotes(
                            sessionId, sessionExerciseId, userId, new NotesUpdateRequestDTO("new")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Session exercise not found");
        }
    }

    @Test
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
    void should_returnWorkoutSessionById_when_sessionExists() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                .withId(sessionId)
                .forUser(UserBuilder.aUser(FAKER).build())
                .build();
        WorkoutSessionResponseDTO response =
                new WorkoutSessionResponseDTO(sessionId, SessionStatus.IN_PROGRESS, null, "notes", List.of());

        when(workoutSessionRepository.findWithExercisesAndSetsByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.of(session));
        when(workoutSessionMapper.toWorkoutSessionResponseDTO(session)).thenReturn(response);

        WorkoutSessionResponseDTO result = workoutSessionService.getWorkoutSessionById(sessionId, userId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void should_throwResourceNotFoundException_when_workoutSessionByIdDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(workoutSessionRepository.findWithExercisesAndSetsByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutSessionService.getWorkoutSessionById(sessionId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Workout session not found");
    }

    @Test
    void should_createWorkoutSessionFromRoutinePreservingExercisesAndDefaultSets() {
        UUID userId = UUID.randomUUID();
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(userId).build();
        User userReference = UserBuilder.aUser(FAKER).withId(userId).build();
        Exercise firstExercise = ExerciseBuilder.anExercise(FAKER).build();
        firstExercise.setId(UUID.randomUUID());
        Exercise secondExercise = ExerciseBuilder.anExercise(FAKER).withCategory(Category.CARDIO).build();
        secondExercise.setId(UUID.randomUUID());
        Routine routine = RoutineBuilder.aRoutine(FAKER).withId(routineId).forUser(user).withExercises(List.of()).build();
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
                UUID.randomUUID(), SessionStatus.IN_PROGRESS, null, "created", List.of());

        when(routineRepository.findByIdAndUserIdAndActiveTrue(routineId, userId)).thenReturn(Optional.of(routine));
        when(userRepository.getReferenceById(userId)).thenReturn(userReference);
        when(workoutSessionRepository.save(any(WorkoutSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutSessionMapper.toWorkoutSessionResponseDTO(any(WorkoutSession.class))).thenReturn(response);

        WorkoutSessionResponseDTO result = workoutSessionService.createWorkoutSessionFromRoutine(routineId, userId);

        ArgumentCaptor<WorkoutSession> savedSessionCaptor = ArgumentCaptor.forClass(WorkoutSession.class);
        verify(workoutSessionRepository).save(savedSessionCaptor.capture());
        WorkoutSession savedSession = savedSessionCaptor.getValue();

        assertThat(result).isEqualTo(response);
        assertThat(savedSession.getUser()).isEqualTo(userReference);
        assertThat(savedSession.getRoutine()).isEqualTo(routine);
        assertThat(savedSession.getExercises()).hasSize(2);
        assertThat(savedSession.getExercises()).extracting(SessionExercise::getPosition).containsExactly(1, 2);
        assertThat(savedSession.getExercises().get(0).getSets()).hasSize(2);
        assertThat(savedSession.getExercises().get(0).getSets()).extracting(SessionSet::getSetNumber).containsExactly(1, 2);
        assertThat(savedSession.getExercises().get(0).getSets()).extracting(SessionSet::getReps).containsExactly(10, 10);
        assertThat(savedSession.getExercises().get(0).getSets()).extracting(SessionSet::getWeightKg).containsExactly(20.0, 20.0);
        assertThat(savedSession.getExercises().get(1).getSets()).hasSize(1);
        assertThat(savedSession.getExercises().get(1).getSets().get(0).getDurationSeconds()).isEqualTo(60);
        assertThat(savedSession.getExercises().get(1).getSets().get(0).getDistanceKm()).isEqualTo(1.2);
    }

    @Test
    void should_throwResourceNotFoundException_when_creatingWorkoutSessionFromMissingRoutine() {
        UUID userId = UUID.randomUUID();
        UUID routineId = UUID.randomUUID();

        when(routineRepository.findByIdAndUserIdAndActiveTrue(routineId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutSessionService.createWorkoutSessionFromRoutine(routineId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Routine not found");
        verify(workoutSessionRepository, never()).save(any(WorkoutSession.class));
    }

    @Test
    void should_createWorkoutSessionFromScratchWithSequentialPositions() {
        UUID userId = UUID.randomUUID();
        UUID firstExerciseId = UUID.randomUUID();
        UUID secondExerciseId = UUID.randomUUID();
        User userReference = UserBuilder.aUser(FAKER).withId(userId).build();
        Exercise firstExercise = ExerciseBuilder.anExercise(FAKER).withCategory(Category.STRENGTH).build();
        Exercise secondExercise = ExerciseBuilder.anExercise(FAKER).withCategory(Category.CARDIO).build();
        firstExercise.setId(firstExerciseId);
        secondExercise.setId(secondExerciseId);
        SessionExerciseRequestDTO firstExerciseRequest = new SessionExerciseRequestDTO(
                99,
                "first notes",
                firstExerciseId,
                List.of(new SessionSetRequestDTO(99, 8, 40.0, null, null, true)));
        SessionExerciseRequestDTO secondExerciseRequest = new SessionExerciseRequestDTO(
                5,
                "second notes",
                secondExerciseId,
                List.of(new SessionSetRequestDTO(null, null, null, 120, null, false)));
        WorkoutSessionRequestDTO request = new WorkoutSessionRequestDTO(
                SessionStatus.IN_PROGRESS,
                null,
                "session notes",
                List.of(firstExerciseRequest, secondExerciseRequest));
        WorkoutSessionResponseDTO response =
                new WorkoutSessionResponseDTO(UUID.randomUUID(), SessionStatus.IN_PROGRESS, null, "session notes", List.of());

        when(exerciseFinder.findActiveByIds(eq(List.of(firstExerciseId, secondExerciseId)), any()))
                .thenReturn(Map.of(firstExerciseId, firstExercise, secondExerciseId, secondExercise));
        when(userRepository.getReferenceById(userId)).thenReturn(userReference);
        when(workoutSessionRepository.save(any(WorkoutSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutSessionMapper.toWorkoutSessionResponseDTO(any(WorkoutSession.class))).thenReturn(response);

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
        assertThat(savedSession.getExercises()).extracting(SessionExercise::getPosition).containsExactly(1, 2);
        assertThat(savedSession.getExercises()).extracting(se -> se.getExercise().getId())
                .containsExactly(firstExerciseId, secondExerciseId);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getSetNumber()).isEqualTo(1);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getReps()).isEqualTo(8);
        assertThat(savedSession.getExercises().get(0).getSets().get(0).getWeightKg()).isEqualTo(40.0);
        assertThat(savedSession.getExercises().get(1).getSets().get(0).getSetNumber()).isEqualTo(1);
        assertThat(savedSession.getExercises().get(1).getSets().get(0).getDurationSeconds()).isEqualTo(120);
        assertThat(savedSession.getExercises().get(1).getSets().get(0).isCompleted()).isFalse();
    }

    @Test
    void should_throwValidationException_when_creatingWorkoutSessionFromScratchWithExerciseErrors() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        WorkoutSessionRequestDTO request = new WorkoutSessionRequestDTO(
                SessionStatus.IN_PROGRESS,
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
    void should_updateNotes_when_sessionIsActive() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                .withId(sessionId)
                .forUser(UserBuilder.aUser(FAKER).build())
                .withNotes("old")
                .build();

        when(workoutSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        workoutSessionService.updateWorkoutSessionNotes(sessionId, userId, new NotesUpdateRequestDTO("new"));

        assertThat(session.getNotes()).isEqualTo("new");
    }

    @Test
    void should_throwBadRequestException_when_updatingNotesForCompletedSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                .withId(sessionId)
                .withStatus(SessionStatus.COMPLETED)
                .forUser(UserBuilder.aUser(FAKER).build())
                .build();

        when(workoutSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> workoutSessionService.updateWorkoutSessionNotes(
                        sessionId, userId, new NotesUpdateRequestDTO("new")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void should_throwResourceNotFoundException_when_updatingNotesForMissingSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(workoutSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutSessionService.updateWorkoutSessionNotes(
                        sessionId, userId, new NotesUpdateRequestDTO("new")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Workout session not found");
    }

    @Test
    void should_completeSession_when_sessionIsActive() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                .withId(sessionId)
                .forUser(UserBuilder.aUser(FAKER).build())
                .build();

        when(workoutSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        workoutSessionService.completeWorkoutSession(sessionId, userId);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getCompletedAt()).isNotNull();
    }

    @Test
    void should_throwBadRequestException_when_completingAlreadyCompletedSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                .withId(sessionId)
                .withStatus(SessionStatus.COMPLETED)
                .forUser(UserBuilder.aUser(FAKER).build())
                .build();

        when(workoutSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> workoutSessionService.completeWorkoutSession(sessionId, userId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void should_throwResourceNotFoundException_when_completingMissingSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(workoutSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutSessionService.completeWorkoutSession(sessionId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Workout session not found");
    }

    @Test
    void should_deleteSession_when_sessionExists() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = WorkoutSessionBuilder.aWorkoutSession(FAKER)
                .withId(sessionId)
                .forUser(UserBuilder.aUser(FAKER).build())
                .build();

        when(workoutSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        workoutSessionService.deleteWorkoutSession(sessionId, userId);

        verify(workoutSessionRepository).delete(session);
    }

    @Test
    void should_throwResourceNotFoundException_when_deletingMissingSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(workoutSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutSessionService.deleteWorkoutSession(sessionId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Workout session not found");
    }
}
