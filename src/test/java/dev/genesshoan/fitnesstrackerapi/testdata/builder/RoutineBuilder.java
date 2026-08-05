package dev.genesshoan.fitnesstrackerapi.testdata.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.routine.domain.RoutineExercise;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import net.datafaker.Faker;

public class RoutineBuilder {

    private UUID id;
    private String name;
    private String description;
    private boolean active = true;
    private User user;
    private List<RoutineExercise> exercises = new ArrayList<>();

    public RoutineBuilder(Faker faker) {
        this.id = UUID.randomUUID();
        this.name =
                faker.funnyName().name() + "-" + UUID.randomUUID().toString().substring(0, 8);
        this.description = faker.lorem().sentence();
    }

    public static RoutineBuilder aRoutine(Faker faker) {
        return new RoutineBuilder(faker);
    }

    public RoutineBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public RoutineBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public RoutineBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public RoutineBuilder withActive(boolean active) {
        this.active = active;
        return this;
    }

    public RoutineBuilder forUser(User user) {
        this.user = user;
        return this;
    }

    public RoutineBuilder withExercises(List<RoutineExercise> exercises) {
        this.exercises = exercises != null ? new ArrayList<>(exercises) : new ArrayList<>();
        return this;
    }

    public RoutineBuilder addExercise(RoutineExercise exercise) {
        if (this.exercises == null) {
            this.exercises = new ArrayList<>();
        }
        this.exercises.add(exercise);
        return this;
    }

    public Routine build() {
        if (user == null) {
            throw new IllegalStateException("User must be set to Routine");
        }

        Routine routine = Routine.builder()
                .id(id)
                .name(name)
                .description(description)
                .active(active)
                .user(user)
                .exercises(exercises)
                .build();

        if (exercises != null) {
            exercises.forEach(ex -> ex.setRoutine(routine));
        }

        return routine;
    }
}
