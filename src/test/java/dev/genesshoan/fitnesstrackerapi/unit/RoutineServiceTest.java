package dev.genesshoan.fitnesstrackerapi.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.UnauthorizedException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ValidationException;
import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.routine.RoutineRepository;
import dev.genesshoan.fitnesstrackerapi.routine.RoutineService;
import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.routine.domain.RoutineExercise;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineExerciseRequestDTO;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineListItemDTO;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineRequestDTO;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineResponseDTO;
import dev.genesshoan.fitnesstrackerapi.routine.mapper.RoutineMapper;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.RoutineBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.UserBuilder;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.DisplayName;
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

@ExtendWith(MockitoExtension.class)
public class RoutineServiceTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private RoutineRepository routineRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private RoutineMapper routineMapper;

    @InjectMocks
    private RoutineService routineService;

    @Test
    @DisplayName("Should return an exercise when the user owns it")
    void getRoutineId_shouldReturnAnExerciseIfUserIsOwner() {
        UUID routineId = UUID.randomUUID();

        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(routineId);

        RoutineResponseDTO dto =
                new RoutineResponseDTO(routine.getId(), routine.getName(), routine.getDescription(), List.of());

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));
        when(routineMapper.toRoutineResponseDTO(routine)).thenReturn(dto);

        RoutineResponseDTO result = routineService.getRoutineById(routineId, user);

        assertThat(result).isEqualTo(dto);

        verify(routineRepository).findByIdAndActiveTrue(routineId);
        verify(routineMapper).toRoutineResponseDTO(routine);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException if id does not exist")
    void getRoutineId_shouldThrowResourceNotFoundException_ifIdDoesNotExists() {
        UUID routineId = UUID.randomUUID();

        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.getRoutineById(routineId, user))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(routineRepository).findByIdAndActiveTrue(routineId);
        verify(routineMapper, never()).toRoutineResponseDTO(any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException if the user does not own the routine")
    void getRoutineId_shouldUnauthorizedException_ifTheUserIsNotTheOwner() {
        UUID routineId = UUID.randomUUID();
        UUID routineUserId = UUID.randomUUID();

        User routineUser = UserBuilder.aUser(FAKER).withId(routineUserId).build();
        User requestUser = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(routineUser).build();

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));

        assertThatThrownBy(() -> routineService.getRoutineById(routineId, requestUser))
                .isInstanceOf(UnauthorizedException.class);

        verify(routineRepository).findByIdAndActiveTrue(routineId);
        verify(routineMapper, never()).toRoutineResponseDTO(any());
    }

    @Test
    @DisplayName("Should create and return a new routine preserving exercise order")
    void createRoutine_shouldCreateAndReturnARoutine() {
        UUID userId = UUID.randomUUID();

        User user = UserBuilder.aUser(FAKER).withId(userId).build();

        Exercise ex1 = ExerciseBuilder.anExercise(FAKER).build();
        ex1.setId(UUID.randomUUID());

        Exercise ex2 = ExerciseBuilder.anExercise(FAKER).build();
        ex2.setId(UUID.randomUUID());

        Exercise ex3 = ExerciseBuilder.anExercise(FAKER).build();
        ex3.setId(UUID.randomUUID());

        List<RoutineExerciseRequestDTO> exercisesDTO = List.of(
                new RoutineExerciseRequestDTO(ex3.getId(), 0, 3, 12, 14.0, null, null, "notes"),
                new RoutineExerciseRequestDTO(ex1.getId(), 0, 3, 12, 14.0, null, null, "notes"),
                new RoutineExerciseRequestDTO(ex2.getId(), 0, 3, 12, 14.0, null, null, "notes"));

        RoutineRequestDTO request = new RoutineRequestDTO("Push Day", "Description", exercisesDTO);

        RoutineResponseDTO response = new RoutineResponseDTO(UUID.randomUUID(), "Push Day", "Description", List.of());

        when(routineRepository.existsByNameAndUserIdAndActiveTrue(request.name(), user.getId()))
                .thenReturn(false);

        when(routineRepository.save(any(Routine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(exerciseRepository.findAllByIdInAndActiveTrue(anySet())).thenReturn(List.of(ex1, ex2, ex3));

        when(routineMapper.toRoutineResponseDTO(any(Routine.class))).thenReturn(response);

        RoutineResponseDTO result = routineService.createRoutine(request, user);

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Routine> captor = ArgumentCaptor.forClass(Routine.class);

        verify(routineRepository).save(any(Routine.class));
        verify(exerciseRepository).findAllByIdInAndActiveTrue(anySet());
        verify(routineMapper).toRoutineResponseDTO(captor.capture());

        List<RoutineExercise> capturedRoutineExercises = captor.getValue().getExercises();

        assertThat(capturedRoutineExercises).hasSize(3);

        assertThat(capturedRoutineExercises)
                .extracting(re -> re.getExercise().getId())
                .containsExactly(ex3.getId(), ex1.getId(), ex2.getId());

        assertThat(capturedRoutineExercises)
                .extracting(RoutineExercise::getPosition)
                .containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName(
            "Should throw a ResourceAlreadyExistsException when already exists an active routine with the same name"
                    + " for the same user")
    void createRoutine_shouldThrowResourceAlreadyExistsException_WhenExists() {
        RoutineRequestDTO request = new RoutineRequestDTO("Push Day", "Description", List.of());
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        when(routineRepository.existsByNameAndUserIdAndActiveTrue(anyString(), any(UUID.class)))
                .thenReturn(true);

        assertThatThrownBy(() -> routineService.createRoutine(request, user))
                .isInstanceOf(ResourceAlreadyExistsException.class);

        verify(routineRepository, never()).save(any(Routine.class));
    }

    @Test
    @DisplayName("Should throw a BadRequestException when one exercise does not exists")
    void createRoutine_shouldThrowBadRequestException_WhenOnExerciseDoesNotExists() {
        UUID userId = UUID.randomUUID();

        User user = UserBuilder.aUser(FAKER).withId(userId).build();

        Exercise ex1 = ExerciseBuilder.anExercise(FAKER).build();
        ex1.setId(UUID.randomUUID());

        Exercise ex2 = ExerciseBuilder.anExercise(FAKER).build();
        ex2.setId(UUID.randomUUID());

        Exercise ex3 = ExerciseBuilder.anExercise(FAKER).build();
        ex3.setId(UUID.randomUUID());

        List<RoutineExerciseRequestDTO> exercisesDTO = List.of(
                new RoutineExerciseRequestDTO(ex1.getId(), 0, 3, 12, 14.0, null, null, "notes"),
                new RoutineExerciseRequestDTO(ex2.getId(), 0, 3, 12, 14.0, null, null, "notes"),
                new RoutineExerciseRequestDTO(ex3.getId(), 0, 3, 12, 14.0, null, null, "notes"));

        RoutineRequestDTO request = new RoutineRequestDTO("Push Day", "Description", exercisesDTO);

        when(routineRepository.existsByNameAndUserIdAndActiveTrue(request.name(), user.getId()))
                .thenReturn(false);

        when(exerciseRepository.findAllByIdInAndActiveTrue(anySet())).thenReturn(List.of(ex1, ex2));

        assertThatThrownBy(() -> routineService.createRoutine(request, user)).isInstanceOf(BadRequestException.class);

        verify(routineRepository).save(any(Routine.class));
        verify(exerciseRepository).findAllByIdInAndActiveTrue(anySet());
        verify(routineMapper, never()).toRoutineResponseDTO(any(Routine.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when the given exercise data does not match its category")
    void createRoutine_ShouldThrowValidationException_WhenDataNotMatchesCategory() {
        UUID userId = UUID.randomUUID();

        User user = UserBuilder.aUser(FAKER).withId(userId).build();

        Exercise ex1 = ExerciseBuilder.anExercise(FAKER).build();
        ex1.setId(UUID.randomUUID());

        Exercise ex2 = ExerciseBuilder.anExercise(FAKER).build();
        ex2.setId(UUID.randomUUID());

        Exercise ex3 = ExerciseBuilder.anExercise(FAKER).build();
        ex3.setId(UUID.randomUUID());

        List<RoutineExerciseRequestDTO> exercisesDTO = List.of(
                new RoutineExerciseRequestDTO(ex1.getId(), 0, 3, 12, 14.0, null, null, "notes"),
                new RoutineExerciseRequestDTO(ex2.getId(), 0, 3, 12, 14.0, 30, 20.5, "notes"),
                new RoutineExerciseRequestDTO(ex3.getId(), 0, 3, 12, 14.0, null, null, "notes"));

        RoutineRequestDTO request = new RoutineRequestDTO("Push Day", "Description", exercisesDTO);

        when(routineRepository.existsByNameAndUserIdAndActiveTrue(request.name(), user.getId()))
                .thenReturn(false);

        when(exerciseRepository.findAllByIdInAndActiveTrue(anySet())).thenReturn(List.of(ex1, ex2, ex3));

        assertThatThrownBy(() -> routineService.createRoutine(request, user))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> {
                    ValidationException e = (ValidationException) ex;

                    assertThat(e.getErrors()).hasSize(1);
                    assertThat(e.getErrors())
                            .containsEntry(ex2.getId().toString(), List.of("Invalid data for category: STRENGTH"));
                });

        verify(exerciseRepository).findAllByIdInAndActiveTrue(anySet());
        verify(routineMapper, never()).toRoutineResponseDTO(any(Routine.class));
    }

    @Test
    @DisplayName("Should update name and description and replace exercises")
    void updateRoutine_shouldUpdateNameAndDescriptionAndReplaceExercises() {
        UUID userId = UUID.randomUUID();
        UUID routineId = UUID.randomUUID();

        User user = UserBuilder.aUser(FAKER).withId(userId).build();

        Exercise ex1 = ExerciseBuilder.anExercise(FAKER).build();
        ex1.setId(UUID.randomUUID());

        Exercise ex2 = ExerciseBuilder.anExercise(FAKER).build();
        ex2.setId(UUID.randomUUID());

        Exercise ex3 = ExerciseBuilder.anExercise(FAKER).build();
        ex3.setId(UUID.randomUUID());

        Routine routine = RoutineBuilder.aRoutine(FAKER)
                .withName("oldName")
                .withDescription("oldDescription")
                .forUser(user)
                .build();
        routine.setExercises(new ArrayList<>(
                List.of(new RoutineExercise(ex1.getId(), 1, 0, 3, 14, 14.0, null, null, "notes", routine, ex1))));
        routine.setId(routineId);

        List<RoutineExerciseRequestDTO> exercisesDTO = new ArrayList<>();
        exercisesDTO.add(new RoutineExerciseRequestDTO(ex2.getId(), 0, 3, 12, 14.0, null, null, "notes"));
        exercisesDTO.add(new RoutineExerciseRequestDTO(ex3.getId(), 0, 3, 12, 14.0, null, null, "notes"));

        RoutineRequestDTO request = new RoutineRequestDTO("newName", "newDescription", exercisesDTO);

        when(routineRepository.existsByNameAndUserIdAndActiveTrue(request.name(), user.getId()))
                .thenReturn(false);

        when(exerciseRepository.findAllByIdInAndActiveTrue(anySet())).thenReturn(List.of(ex2, ex3));

        when(routineMapper.toRoutineResponseDTO(any(Routine.class))).thenReturn(mock(RoutineResponseDTO.class));

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));

        routineService.updateRoutine(routineId, request, user);

        ArgumentCaptor<Routine> captor = ArgumentCaptor.forClass(Routine.class);

        verify(exerciseRepository).findAllByIdInAndActiveTrue(anySet());
        verify(routineMapper).toRoutineResponseDTO(captor.capture());

        Routine captured = captor.getValue();

        assertThat(captured.getName()).isEqualTo("newName");
        assertThat(captured.getDescription()).isEqualTo("newDescription");
        assertThat(captured.getExercises())
                .extracting(RoutineExercise::getExercise)
                .containsExactly(ex2, ex3);
    }

    @Test
    @DisplayName(
            "Should throw ResourceAlreadyExistsException already exists a routine with the given name for the user")
    void updateRoutine_shouldThrowResourceAlreadyExistsException() {
        UUID userId = UUID.randomUUID();
        UUID routineId = UUID.randomUUID();

        User user = UserBuilder.aUser(FAKER).withId(userId).build();
        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();

        RoutineRequestDTO request = new RoutineRequestDTO("name", "description", List.of());

        when(routineRepository.existsByNameAndUserIdAndActiveTrue(request.name(), user.getId()))
                .thenReturn(true);

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));

        assertThatThrownBy(() -> routineService.updateRoutine(routineId, request, user))
                .isInstanceOf(ResourceAlreadyExistsException.class);

        verify(exerciseRepository, never()).findAllByIdInAndActiveTrue(anySet());
        verify(routineMapper, never()).toRoutineResponseDTO(any(Routine.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when routine does not exist")
    void updateRoutine_shouldThrowResourceNotFoundException_WhenRoutineDoesNotExist() {
        UUID routineId = UUID.randomUUID();

        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        RoutineRequestDTO request = new RoutineRequestDTO("Push Day", "Description", List.of());

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.updateRoutine(routineId, request, user))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(exerciseRepository, never()).findAllByIdInAndActiveTrue(anySet());
        verify(routineMapper, never()).toRoutineResponseDTO(any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user does not own routine")
    void updateRoutine_shouldThrowUnauthorizedException_WhenUserIsNotOwner() {
        UUID routineId = UUID.randomUUID();

        User owner = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();
        User requestUser = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(owner).build();

        RoutineRequestDTO request = new RoutineRequestDTO("Push Day", "Description", List.of());

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));

        assertThatThrownBy(() -> routineService.updateRoutine(routineId, request, requestUser))
                .isInstanceOf(UnauthorizedException.class);

        verify(exerciseRepository, never()).findAllByIdInAndActiveTrue(anySet());
        verify(routineMapper, never()).toRoutineResponseDTO(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when the given exercise data is not valid for exercise category")
    void updateRoutine_shouldThrowValidationException_WhenDataNotMatchesCategory() {
        UUID userId = UUID.randomUUID();

        User user = UserBuilder.aUser(FAKER).withId(userId).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(UUID.randomUUID());

        Exercise ex1 = ExerciseBuilder.anExercise(FAKER).build();
        ex1.setId(UUID.randomUUID());

        Exercise ex2 = ExerciseBuilder.anExercise(FAKER).build();
        ex2.setId(UUID.randomUUID());

        Exercise ex3 = ExerciseBuilder.anExercise(FAKER).build();
        ex3.setId(UUID.randomUUID());

        List<RoutineExerciseRequestDTO> exercisesDTO = List.of(
                new RoutineExerciseRequestDTO(ex1.getId(), 0, 3, 12, 14.0, null, null, "notes"),
                new RoutineExerciseRequestDTO(ex2.getId(), 0, 3, 12, 14.0, 30, 20.5, "notes"),
                new RoutineExerciseRequestDTO(ex3.getId(), 0, 3, 12, 14.0, null, null, "notes"));

        RoutineRequestDTO request = new RoutineRequestDTO("Push Day", "Description", exercisesDTO);

        when(routineRepository.existsByNameAndUserIdAndActiveTrue(request.name(), user.getId()))
                .thenReturn(false);

        when(exerciseRepository.findAllByIdInAndActiveTrue(anySet())).thenReturn(List.of(ex1, ex2, ex3));

        when(routineRepository.findByIdAndActiveTrue(routine.getId())).thenReturn(Optional.of(routine));

        assertThatThrownBy(() -> routineService.updateRoutine(routine.getId(), request, user))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> {
                    ValidationException e = (ValidationException) ex;

                    assertThat(e.getErrors()).hasSize(1);
                    assertThat(e.getErrors())
                            .containsEntry(ex2.getId().toString(), List.of("Invalid data for category: STRENGTH"));
                });

        verify(exerciseRepository).findAllByIdInAndActiveTrue(anySet());
        verify(routineMapper, never()).toRoutineResponseDTO(any(Routine.class));
    }

    @Test
    @DisplayName("Should mark the routine as inactive")
    void deleteRoutine_shouldSetActiveAsFalse() {
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        UUID routineId = UUID.randomUUID();

        Routine routine = RoutineBuilder.aRoutine(FAKER)
                .withActive(true)
                .forUser(user)
                .build();
        routine.setId(routineId);

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));

        routineService.deleteRoutine(routineId, user);

        assertThat(routine.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when routine does not exist")
    void deleteRoutine_shouldThrowResourceNotFoundException_WhenRoutineDoesNotExist() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).build();

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.deleteRoutine(routineId, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // TODO: Implement test for deleteRoutine when the given routine has an active training session

    @Test
    @DisplayName("Should add exercise at the given position and shift subsequent exercises by one")
    void addRoutineExercise_shouldAddAndShiftPositions_WhenPositionIsValid() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(routineId);

        Exercise ex1 = ExerciseBuilder.anExercise(FAKER).build();
        ex1.setId(UUID.randomUUID());
        Exercise ex2 = ExerciseBuilder.anExercise(FAKER).build();
        ex2.setId(UUID.randomUUID());
        Exercise ex3 = ExerciseBuilder.anExercise(FAKER).build();
        ex3.setId(UUID.randomUUID());
        Exercise newExercise = ExerciseBuilder.anExercise(FAKER).build();
        newExercise.setId(UUID.randomUUID());

        routine.setExercises(new ArrayList<>(List.of(
                routineExerciseAt(1, ex1, routine),
                routineExerciseAt(2, ex2, routine),
                routineExerciseAt(3, ex3, routine))));

        RoutineExerciseRequestDTO dto =
                new RoutineExerciseRequestDTO(newExercise.getId(), 0, 3, 12, 14.0, null, null, "notes");

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));
        when(exerciseRepository.findById(newExercise.getId())).thenReturn(Optional.of(newExercise));
        when(routineMapper.toRoutineResponseDTO(routine)).thenReturn(mock(RoutineResponseDTO.class));

        routineService.addRoutineExercise(routineId, 2, dto, user);

        assertThat(routine.getExercises())
                .extracting(RoutineExercise::getExercise, RoutineExercise::getPosition)
                .containsExactlyInAnyOrder(
                        tuple(ex1, 1), tuple(newExercise, 2), tuple(ex2, 3), tuple(ex3, 4));
    }

    @Test
    @DisplayName("Should clamp position to maxPosition + 1 when the requested position exceeds the routine size")
    void addRoutineExercise_shouldClampToEnd_WhenPositionExceedsMax() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(routineId);

        Exercise ex1 = ExerciseBuilder.anExercise(FAKER).build();
        ex1.setId(UUID.randomUUID());
        Exercise ex2 = ExerciseBuilder.anExercise(FAKER).build();
        ex2.setId(UUID.randomUUID());
        Exercise newExercise = ExerciseBuilder.anExercise(FAKER).build();
        newExercise.setId(UUID.randomUUID());

        routine.setExercises(new ArrayList<>(
                List.of(routineExerciseAt(1, ex1, routine), routineExerciseAt(2, ex2, routine))));

        RoutineExerciseRequestDTO dto =
                new RoutineExerciseRequestDTO(newExercise.getId(), 0, 3, 12, 14.0, null, null, "notes");

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));
        when(exerciseRepository.findById(newExercise.getId())).thenReturn(Optional.of(newExercise));
        when(routineMapper.toRoutineResponseDTO(routine)).thenReturn(mock(RoutineResponseDTO.class));

        routineService.addRoutineExercise(routineId, 10, dto, user);

        assertThat(routine.getExercises())
                .extracting(RoutineExercise::getExercise, RoutineExercise::getPosition)
                .containsExactlyInAnyOrder(tuple(ex1, 1), tuple(ex2, 2), tuple(newExercise, 3));
    }

    @Test
    @DisplayName("Should clamp position to 1 when the requested position is lower than 1")
    void addRoutineExercise_shouldClampToStart_WhenPositionIsLowerThanOne() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(routineId);

        Exercise ex1 = ExerciseBuilder.anExercise(FAKER).build();
        ex1.setId(UUID.randomUUID());
        Exercise ex2 = ExerciseBuilder.anExercise(FAKER).build();
        ex2.setId(UUID.randomUUID());
        Exercise newExercise = ExerciseBuilder.anExercise(FAKER).build();
        newExercise.setId(UUID.randomUUID());

        routine.setExercises(new ArrayList<>(
                List.of(routineExerciseAt(1, ex1, routine), routineExerciseAt(2, ex2, routine))));

        RoutineExerciseRequestDTO dto =
                new RoutineExerciseRequestDTO(newExercise.getId(), 0, 3, 12, 14.0, null, null, "notes");

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));
        when(exerciseRepository.findById(newExercise.getId())).thenReturn(Optional.of(newExercise));
        when(routineMapper.toRoutineResponseDTO(routine)).thenReturn(mock(RoutineResponseDTO.class));

        routineService.addRoutineExercise(routineId, -5, dto, user);

        assertThat(routine.getExercises())
                .extracting(RoutineExercise::getExercise, RoutineExercise::getPosition)
                .containsExactlyInAnyOrder(tuple(newExercise, 1), tuple(ex1, 2), tuple(ex2, 3));
    }

    @Test
    @DisplayName("Should add at the end without shifting anything when position equals maxPosition + 1 exactly")
    void addRoutineExercise_shouldAddAtEndWithoutShift_WhenPositionIsExactlyMaxPositionPlusOne() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(routineId);

        Exercise ex1 = ExerciseBuilder.anExercise(FAKER).build();
        ex1.setId(UUID.randomUUID());
        Exercise ex2 = ExerciseBuilder.anExercise(FAKER).build();
        ex2.setId(UUID.randomUUID());
        Exercise newExercise = ExerciseBuilder.anExercise(FAKER).build();
        newExercise.setId(UUID.randomUUID());

        routine.setExercises(new ArrayList<>(
                List.of(routineExerciseAt(1, ex1, routine), routineExerciseAt(2, ex2, routine))));

        RoutineExerciseRequestDTO dto =
                new RoutineExerciseRequestDTO(newExercise.getId(), 0, 3, 12, 14.0, null, null, "notes");

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));
        when(exerciseRepository.findById(newExercise.getId())).thenReturn(Optional.of(newExercise));
        when(routineMapper.toRoutineResponseDTO(routine)).thenReturn(mock(RoutineResponseDTO.class));

        routineService.addRoutineExercise(routineId, 3, dto, user);

        assertThat(routine.getExercises())
                .extracting(RoutineExercise::getExercise, RoutineExercise::getPosition)
                .containsExactlyInAnyOrder(tuple(ex1, 1), tuple(ex2, 2), tuple(newExercise, 3));
    }

    @Test
    @DisplayName("Should add exercise to an empty routine at position 1 without shifting anything")
    void addRoutineExercise_shouldAddToEmptyRoutine() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(routineId);
        routine.setExercises(new ArrayList<>());

        Exercise newExercise = ExerciseBuilder.anExercise(FAKER).build();
        newExercise.setId(UUID.randomUUID());

        RoutineExerciseRequestDTO dto =
                new RoutineExerciseRequestDTO(newExercise.getId(), 0, 3, 12, 14.0, null, null, "notes");

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));
        when(exerciseRepository.findById(newExercise.getId())).thenReturn(Optional.of(newExercise));
        when(routineMapper.toRoutineResponseDTO(routine)).thenReturn(mock(RoutineResponseDTO.class));

        routineService.addRoutineExercise(routineId, 1, dto, user);

        assertThat(routine.getExercises())
                .extracting(RoutineExercise::getExercise, RoutineExercise::getPosition)
                .containsExactly(tuple(newExercise, 1));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when the exercise to add does not exist")
    void addRoutineExercise_shouldThrowResourceNotFoundException_WhenExerciseDoesNotExist() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(routineId);
        routine.setExercises(new ArrayList<>());

        UUID missingExerciseId = UUID.randomUUID();
        RoutineExerciseRequestDTO dto =
                new RoutineExerciseRequestDTO(missingExerciseId, 0, 3, 12, 14.0, null, null, "notes");

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));
        when(exerciseRepository.findById(missingExerciseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.addRoutineExercise(routineId, 1, dto, user))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(routineMapper, never()).toRoutineResponseDTO(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when the routine does not exist")
    void addRoutineExercise_shouldThrowResourceNotFoundException_WhenRoutineDoesNotExist() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        RoutineExerciseRequestDTO dto =
                new RoutineExerciseRequestDTO(UUID.randomUUID(), 0, 3, 12, 14.0, null, null, "notes");

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.addRoutineExercise(routineId, 1, dto, user))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(exerciseRepository, never()).findById(any());
        verify(routineMapper, never()).toRoutineResponseDTO(any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when the user does not own the routine")
    void addRoutineExercise_shouldThrowUnauthorizedException_WhenUserIsNotOwner() {
        UUID routineId = UUID.randomUUID();
        User owner = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();
        User requestUser = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(owner).build();
        routine.setId(routineId);

        RoutineExerciseRequestDTO dto =
                new RoutineExerciseRequestDTO(UUID.randomUUID(), 0, 3, 12, 14.0, null, null, "notes");

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));

        assertThatThrownBy(() -> routineService.addRoutineExercise(routineId, 1, dto, requestUser))
                .isInstanceOf(UnauthorizedException.class);

        verify(exerciseRepository, never()).findById(any());
        verify(routineMapper, never()).toRoutineResponseDTO(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when the given exercise data does not match its category")
    void addRoutineExercise_shouldThrowValidationException_WhenDataNotMatchesCategory() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(routineId);
        routine.setExercises(new ArrayList<>());

        Exercise exercise = ExerciseBuilder.anExercise(FAKER).build();
        exercise.setId(UUID.randomUUID());

        RoutineExerciseRequestDTO dto =
                new RoutineExerciseRequestDTO(exercise.getId(), 0, 3, 12, 14.0, 30, 20.5, "notes");

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));
        when(exerciseRepository.findById(exercise.getId())).thenReturn(Optional.of(exercise));

        assertThatThrownBy(() -> routineService.addRoutineExercise(routineId, 1, dto, user))
                .isInstanceOf(ValidationException.class);

        verify(routineMapper, never()).toRoutineResponseDTO(any());
    }

    @Test
    @DisplayName("Should remove the exercise at the given position and shift subsequent positions by one")
    void deleteRoutineExercise_shouldRemoveAndShiftPositions() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(routineId);

        Exercise ex1 = ExerciseBuilder.anExercise(FAKER).build();
        ex1.setId(UUID.randomUUID());
        Exercise ex2 = ExerciseBuilder.anExercise(FAKER).build();
        ex2.setId(UUID.randomUUID());
        Exercise ex3 = ExerciseBuilder.anExercise(FAKER).build();
        ex3.setId(UUID.randomUUID());

        routine.setExercises(new ArrayList<>(List.of(
                routineExerciseAt(1, ex1, routine),
                routineExerciseAt(2, ex2, routine),
                routineExerciseAt(3, ex3, routine))));

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));

        routineService.deleteRoutineExercise(routineId, 1, user);

        assertThat(routine.getExercises())
                .extracting(RoutineExercise::getExercise, RoutineExercise::getPosition)
                .containsExactlyInAnyOrder(tuple(ex2, 1), tuple(ex3, 2));
    }

    @Test
    @DisplayName("Should leave the routine empty when deleting its only exercise")
    void deleteRoutineExercise_shouldResultInEmptyRoutine_WhenDeletingLastExercise() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(routineId);

        Exercise ex1 = ExerciseBuilder.anExercise(FAKER).build();
        ex1.setId(UUID.randomUUID());

        routine.setExercises(new ArrayList<>(List.of(routineExerciseAt(1, ex1, routine))));

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));

        routineService.deleteRoutineExercise(routineId, 1, user);

        assertThat(routine.getExercises()).isEmpty();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when there is no exercise at the given position")
    void deleteRoutineExercise_shouldThrowResourceNotFoundException_WhenNoExerciseAtPosition() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(routineId);

        Exercise ex1 = ExerciseBuilder.anExercise(FAKER).build();
        ex1.setId(UUID.randomUUID());

        routine.setExercises(new ArrayList<>(List.of(routineExerciseAt(1, ex1, routine))));

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));

        assertThatThrownBy(() -> routineService.deleteRoutineExercise(routineId, 5, user))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(routine.getExercises()).hasSize(1);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when the routine does not exist")
    void deleteRoutineExercise_shouldThrowResourceNotFoundException_WhenRoutineDoesNotExist() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.deleteRoutineExercise(routineId, 1, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when the user does not own the routine")
    void deleteRoutineExercise_shouldThrowUnauthorizedException_WhenUserIsNotOwner() {
        UUID routineId = UUID.randomUUID();
        User owner = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();
        User requestUser = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(owner).build();
        routine.setId(routineId);

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));

        assertThatThrownBy(() -> routineService.deleteRoutineExercise(routineId, 1, requestUser))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should replace the exercise at the given position keeping the same position")
    void updateRoutineExercise_shouldReplaceExercise_KeepingSamePosition() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(routineId);

        Exercise ex1 = ExerciseBuilder.anExercise(FAKER).build();
        ex1.setId(UUID.randomUUID());
        Exercise ex2 = ExerciseBuilder.anExercise(FAKER).build();
        ex2.setId(UUID.randomUUID());

        routine.setExercises(new ArrayList<>(List.of(routineExerciseAt(2, ex1, routine))));

        RoutineExerciseRequestDTO dto = new RoutineExerciseRequestDTO(ex2.getId(), 0, 3, 12, 14.0, null, null, "notes");

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));
        when(exerciseRepository.findById(ex2.getId())).thenReturn(Optional.of(ex2));
        when(routineMapper.toRoutineResponseDTO(routine)).thenReturn(mock(RoutineResponseDTO.class));

        routineService.updateRoutineExercise(routineId, 2, dto, user);

        assertThat(routine.getExercises())
                .extracting(RoutineExercise::getExercise, RoutineExercise::getPosition)
                .containsExactly(tuple(ex2, 2));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when there is no exercise at the given position")
    void updateRoutineExercise_shouldThrowResourceNotFoundException_WhenNoExerciseAtPosition() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(routineId);
        routine.setExercises(new ArrayList<>());

        Exercise exercise = ExerciseBuilder.anExercise(FAKER).build();
        exercise.setId(UUID.randomUUID());

        RoutineExerciseRequestDTO dto =
                new RoutineExerciseRequestDTO(exercise.getId(), 0, 3, 12, 14.0, null, null, "notes");

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));
        when(exerciseRepository.findById(exercise.getId())).thenReturn(Optional.of(exercise));

        assertThatThrownBy(() -> routineService.updateRoutineExercise(routineId, 3, dto, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when the new exercise does not exist")
    void updateRoutineExercise_shouldThrowResourceNotFoundException_WhenNewExerciseDoesNotExist() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(routineId);
        routine.setExercises(new ArrayList<>());

        UUID missingExerciseId = UUID.randomUUID();
        RoutineExerciseRequestDTO dto =
                new RoutineExerciseRequestDTO(missingExerciseId, 0, 3, 12, 14.0, null, null, "notes");

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));
        when(exerciseRepository.findById(missingExerciseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.updateRoutineExercise(routineId, 1, dto, user))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(routineMapper, never()).toRoutineResponseDTO(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when the given exercise data does not match its category")
    void updateRoutineExercise_shouldThrowValidationException_WhenDataNotMatchesCategory() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(user).build();
        routine.setId(routineId);

        Exercise ex1 = ExerciseBuilder.anExercise(FAKER).build();
        ex1.setId(UUID.randomUUID());
        Exercise ex2 = ExerciseBuilder.anExercise(FAKER).build();
        ex2.setId(UUID.randomUUID());

        routine.setExercises(new ArrayList<>(List.of(routineExerciseAt(1, ex1, routine))));

        RoutineExerciseRequestDTO dto = new RoutineExerciseRequestDTO(ex2.getId(), 0, 3, 12, 14.0, 30, 20.5, "notes");

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));
        when(exerciseRepository.findById(ex2.getId())).thenReturn(Optional.of(ex2));

        assertThatThrownBy(() -> routineService.updateRoutineExercise(routineId, 1, dto, user))
                .isInstanceOf(ValidationException.class);

        verify(routineMapper, never()).toRoutineResponseDTO(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when the routine does not exist")
    void updateRoutineExercise_shouldThrowResourceNotFoundException_WhenRoutineDoesNotExist() {
        UUID routineId = UUID.randomUUID();
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        RoutineExerciseRequestDTO dto =
                new RoutineExerciseRequestDTO(UUID.randomUUID(), 0, 3, 12, 14.0, null, null, "notes");

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.updateRoutineExercise(routineId, 1, dto, user))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(exerciseRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when the user does not own the routine")
    void updateRoutineExercise_shouldThrowUnauthorizedException_WhenUserIsNotOwner() {
        UUID routineId = UUID.randomUUID();
        User owner = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();
        User requestUser = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();

        Routine routine = RoutineBuilder.aRoutine(FAKER).forUser(owner).build();
        routine.setId(routineId);

        RoutineExerciseRequestDTO dto =
                new RoutineExerciseRequestDTO(UUID.randomUUID(), 0, 3, 12, 14.0, null, null, "notes");

        when(routineRepository.findByIdAndActiveTrue(routineId)).thenReturn(Optional.of(routine));

        assertThatThrownBy(() -> routineService.updateRoutineExercise(routineId, 1, dto, requestUser))
                .isInstanceOf(UnauthorizedException.class);

        verify(exerciseRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should return the page returned by the repository for the given user")
    void getRoutinesByUserId_shouldReturnPagedRoutines() {
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();
        Pageable pageable = PageRequest.of(0, 10);

        Page<RoutineListItemDTO> page = new PageImpl<>(List.of(mock(RoutineListItemDTO.class)));

        when(routineRepository.findAllByUserIdAndActiveTrueWithExerciseCount(user.getId(), pageable))
                .thenReturn(page);

        Page<RoutineListItemDTO> result = routineService.getRoutinesByUserId(user, pageable);

        assertThat(result).isEqualTo(page);

        verify(routineRepository).findAllByUserIdAndActiveTrueWithExerciseCount(user.getId(), pageable);
    }

    @Test
    @DisplayName("Should return an empty page when the user has no routines")
    void getRoutinesByUserId_shouldReturnEmptyPage_WhenUserHasNoRoutines() {
        User user = UserBuilder.aUser(FAKER).withId(UUID.randomUUID()).build();
        Pageable pageable = PageRequest.of(0, 10);

        Page<RoutineListItemDTO> emptyPage = Page.empty(pageable);

        when(routineRepository.findAllByUserIdAndActiveTrueWithExerciseCount(user.getId(), pageable))
                .thenReturn(emptyPage);

        Page<RoutineListItemDTO> result = routineService.getRoutinesByUserId(user, pageable);

        assertThat(result).isEmpty();
    }

    private RoutineExercise routineExerciseAt(int position, Exercise exercise, Routine routine) {
        return new RoutineExercise(UUID.randomUUID(), position, 0, 3, 12, 14.0, null, null, "notes", routine, exercise);
    }
}
