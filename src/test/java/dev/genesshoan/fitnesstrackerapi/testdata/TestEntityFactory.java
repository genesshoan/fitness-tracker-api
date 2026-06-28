package dev.genesshoan.fitnesstrackerapi.testdata;

import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ExerciseMuscle;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.MuscleRepository;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.Muscle;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ExerciseBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ExerciseMuscleBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.MuscleBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.datafaker.Faker;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class TestEntityFactory {

    private final ExerciseRepository exerciseRepository;
    private final MuscleRepository muscleRepository;
    private final Faker faker = new Faker();

    public TestEntityFactory(
        ExerciseRepository exerciseRepository,
        MuscleRepository muscleRepository
    ) {
        this.exerciseRepository = exerciseRepository;
        this.muscleRepository = muscleRepository;
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

    /**
     * Creates and persists an Exercise with a specified number of associated Muscles,
     * all having a given impact level.
     */
    public Exercise createAndPersistExerciseWithMuscles(
        int numMuscles,
        ImpactLevel impactLevel
    ) {
        Set<Muscle> muscles = IntStream.range(0, numMuscles)
            .mapToObj(i -> createAndPersistMuscle())
            .collect(Collectors.toSet());

        Exercise exercise = exerciseRepository.save(
            ExerciseBuilder.anExercise(faker).build()
        );

        Set<ExerciseMuscle> exerciseMuscles = muscles
            .stream()
            .map(muscle ->
                ExerciseMuscleBuilder.anExerciseMuscle()
                    .forExercise(exercise)
                    .forMuscle(muscle)
                    .withImpact(impactLevel)
                    .build()
            )
            .collect(Collectors.toSet());

        exercise.setExerciseMuscles(exerciseMuscles);

        return exerciseRepository.save(exercise);
    }

    /**
     * Creates and persists an Exercise with a specific primary Muscle.
     */
    public Exercise createAndPersistExerciseWithOnePrimaryMuscle(
        Muscle muscle
    ) {
        Exercise exercise = exerciseRepository.save(
            ExerciseBuilder.anExercise(faker).build()
        );
        ExerciseMuscle exerciseMuscle = ExerciseMuscleBuilder.anExerciseMuscle()
            .forExercise(exercise)
            .forMuscle(muscle)
            .withImpact(ImpactLevel.PRIMARY)
            .build();
        exercise.setExerciseMuscles(new HashSet<>(Set.of(exerciseMuscle)));
        return exerciseRepository.save(exercise);
    }

    public Exercise createAndPersistExerciseWithMuscles(
        ExerciseBuilder builder,
        List<Muscle> muscles,
        ImpactLevel impactLevel
    ) {
        Exercise exercise = exerciseRepository.save(builder.build());
        Set<ExerciseMuscle> exerciseMuscles = muscles
            .stream()
            .map(muscle ->
                ExerciseMuscleBuilder.anExerciseMuscle()
                    .forExercise(exercise)
                    .forMuscle(muscle)
                    .withImpact(impactLevel)
                    .build()
            )
            .collect(Collectors.toSet());
        exercise.setExerciseMuscles(exerciseMuscles);
        return exerciseRepository.save(exercise);
    }
}
