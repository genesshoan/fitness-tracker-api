package dev.genesshoan.fitnesstrackerapi.testdata;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.boot.test.context.TestComponent;

import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ExerciseMuscle;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.MuscleRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.Muscle;
import dev.genesshoan.fitnesstrackerapi.progressrecord.ProgressRecord;
import dev.genesshoan.fitnesstrackerapi.progressrecord.ProgressRecordRepository;
import dev.genesshoan.fitnesstrackerapi.routine.RoutineRepository;
import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.routine.domain.RoutineExercise;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ExerciseMuscleBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.MuscleBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ProgressRecordBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.RoutineBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.RoutineExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.UserBuilder;
import dev.genesshoan.fitnesstrackerapi.user.UserRepository;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import net.datafaker.Faker;

@TestComponent
public class TestEntityFactory {

    private final ExerciseRepository exerciseRepository;
    private final MuscleRepository muscleRepository;
    private final UserRepository userRepository;
    private final RoutineRepository routineRepository;
    private final ProgressRecordRepository progressRecordRepository;
    private final Faker faker = new Faker();

    public TestEntityFactory(
            ExerciseRepository exerciseRepository,
            MuscleRepository muscleRepository,
            UserRepository userRepository,
            RoutineRepository routineRepository,
            ProgressRecordRepository progressRecordRepository) {
        this.exerciseRepository = exerciseRepository;
        this.muscleRepository = muscleRepository;
        this.userRepository = userRepository;
        this.routineRepository = routineRepository;
        this.progressRecordRepository = progressRecordRepository;
    }

    public Faker faker() {
        return faker;
    }

    public Muscle createAndPersistMuscle(MuscleBuilder builder) {
        return muscleRepository.save(builder.build());
    }

    public Muscle createAndPersistMuscle() {
        return createAndPersistMuscle(MuscleBuilder.aMuscle(faker));
    }

    public Exercise createAndPersistExercise(ExerciseBuilder builder) {
        return exerciseRepository.save(builder.build());
    }

    public Exercise createAndPersistExercise() {
        return createAndPersistExercise(ExerciseBuilder.anExercise(faker));
    }

    public Exercise createAndPersistExerciseWithMuscles(int numMuscles, ImpactLevel impactLevel) {
        Set<Muscle> muscles = IntStream.range(0, numMuscles)
                .mapToObj(i -> createAndPersistMuscle())
                .collect(Collectors.toSet());

        Exercise exercise =
                exerciseRepository.save(ExerciseBuilder.anExercise(faker).build());

        Set<ExerciseMuscle> exerciseMuscles = muscles.stream()
                .map(muscle -> ExerciseMuscleBuilder.anExerciseMuscle()
                        .forExercise(exercise)
                        .forMuscle(muscle)
                        .withImpact(impactLevel)
                        .build())
                .collect(Collectors.toSet());

        exercise.setExerciseMuscles(exerciseMuscles);
        return exerciseRepository.save(exercise);
    }

    public Exercise createAndPersistExerciseWithOnePrimaryMuscle(Muscle muscle) {
        Exercise exercise =
                exerciseRepository.save(ExerciseBuilder.anExercise(faker).build());
        ExerciseMuscle exerciseMuscle = ExerciseMuscleBuilder.anExerciseMuscle()
                .forExercise(exercise)
                .forMuscle(muscle)
                .withImpact(ImpactLevel.PRIMARY)
                .build();
        exercise.setExerciseMuscles(new HashSet<>(Set.of(exerciseMuscle)));
        return exerciseRepository.save(exercise);
    }

    public Exercise createAndPersistExerciseWithMuscles(
            ExerciseBuilder builder, List<Muscle> muscles, ImpactLevel impactLevel) {
        Exercise exercise = exerciseRepository.save(builder.build());
        Set<ExerciseMuscle> exerciseMuscles = muscles.stream()
                .map(muscle -> ExerciseMuscleBuilder.anExerciseMuscle()
                        .forExercise(exercise)
                        .forMuscle(muscle)
                        .withImpact(impactLevel)
                        .build())
                .collect(Collectors.toSet());
        exercise.setExerciseMuscles(exerciseMuscles);
        return exerciseRepository.save(exercise);
    }

    public User createAndPersistUser(UserBuilder builder) {
        return userRepository.save(builder.build());
    }

    public User createAndPersistUser() {
        return createAndPersistUser(UserBuilder.aUser(faker));
    }

    public Routine createAndPersistRoutine(RoutineBuilder builder) {
        Routine routine = builder.build();
        return routineRepository.save(routine);
    }

    public Routine createAndPersistRoutine(User user) {
        return createAndPersistRoutine(RoutineBuilder.aRoutine(faker).forUser(user));
    }

    public Routine createAndPersistRoutine(User user, String name) {
        return createAndPersistRoutine(
                RoutineBuilder.aRoutine(faker).forUser(user).withName(name));
    }

    public RoutineExercise createAndPersistRoutineExercise(RoutineExerciseBuilder builder) {
        RoutineExercise routineExercise = builder.build();

        Routine routine = routineExercise.getRoutine();
        Exercise exercise = routineExercise.getExercise();

        if (routine.getId() == null) {
            routine = routineRepository.save(routine);
            routineExercise.setRoutine(routine);
        }

        if (exercise.getId() == null) {
            exercise = exerciseRepository.save(exercise);
            routineExercise.setExercise(exercise);
        }

        if (routine.getExercises() == null) {
            routine.setExercises(new ArrayList<>());
        }
        routine.getExercises().add(routineExercise);
        routineRepository.save(routine);

        return routineExercise;
    }

    public RoutineExercise createAndPersistRoutineExercise(Routine routine, Exercise exercise, int position) {
        RoutineExercise routineExercise = RoutineExerciseBuilder.aRoutineExercise(faker)
                .forRoutine(routine)
                .forExercise(exercise)
                .withPosition(position)
                .build();

        if (routine.getExercises() == null) {
            routine.setExercises(new ArrayList<>());
        }
        routine.getExercises().add(routineExercise);
        routineRepository.save(routine);

        return routineExercise;
    }

    /**
     * Creates a routine with multiple exercises at specified positions
     */
    public Routine createAndPersistRoutineWithExercises(User user, List<Exercise> exercises) {
        Routine routine = createAndPersistRoutine(user);

        for (int i = 0; i < exercises.size(); i++) {
            createAndPersistRoutineExercise(routine, exercises.get(i), i + 1);
        }

        return routineRepository.findById(routine.getId()).orElseThrow();
    }

    /**
     * Creates a routine with a single exercise
     */
    public Routine createAndPersistRoutineWithExercise(User user, Exercise exercise) {
        return createAndPersistRoutineWithExercises(user, List.of(exercise));
    }

    /**
     * Creates a complete routine with exercises and muscles
     */
    public Routine createAndPersistCompleteRoutine(User user, int numExercises) {
        List<Exercise> exercises = IntStream.range(0, numExercises)
                .mapToObj(i -> createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY))
                .collect(Collectors.toList());

        return createAndPersistRoutineWithExercises(user, exercises);
    }

    /**
     * Creates a routine with exercises at specific positions
     */
    public Routine createAndPersistRoutineWithExercisesAtPositions(
            User user, List<Exercise> exercises, List<Integer> positions) {

        if (exercises.size() != positions.size()) {
            throw new IllegalArgumentException("Exercises and positions must have same size");
        }

        Routine routine = createAndPersistRoutine(user);

        for (int i = 0; i < exercises.size(); i++) {
            createAndPersistRoutineExercise(routine, exercises.get(i), positions.get(i));
        }

        return routineRepository.findById(routine.getId()).orElseThrow();
    }

    public RoutineRepository getRoutineRepository() {
        return routineRepository;
    }

    public Routine refreshRoutine(Routine routine) {
        return routineRepository.findById(routine.getId()).orElseThrow();
    }

    public List<Exercise> createAndPersistExercises(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> createAndPersistExerciseWithMuscles(2, ImpactLevel.PRIMARY))
                .collect(Collectors.toList());
    }

    public ProgressRecord createAndPersistProgressRecord(ProgressRecordBuilder builder) {
        return progressRecordRepository.save(builder.build());
    }

    public ProgressRecord createAndPersistProgressRecord(User user) {
        return createAndPersistProgressRecord(
                ProgressRecordBuilder.aProgressRecord(faker).forUser(user));
    }

    public ProgressRecord createAndPersistProgressRecord(User user, LocalDate recordedAt) {
        return createAndPersistProgressRecord(
                ProgressRecordBuilder.aProgressRecord(faker).forUser(user).withRecordedAt(recordedAt));
    }
}
