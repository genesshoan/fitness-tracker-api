package dev.genesshoan.fitnesstrackerapi.testdata.builder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionExercise;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionStatus;
import dev.genesshoan.fitnesstrackerapi.workout.domain.WorkoutSession;
import net.datafaker.Faker;

public class WorkoutSessionBuilder {

    private UUID id;
    private SessionStatus status = SessionStatus.IN_PROGRESS;
    private Instant startedAt = Instant.now();
    private Instant completedAt;
    private String notes;
    private User user;
    private Routine routine;
    private List<SessionExercise> exercises = new ArrayList<>();

    public WorkoutSessionBuilder(Faker faker) {
        this.id = UUID.randomUUID();
        this.notes = faker.lorem().sentence();
    }

    public static WorkoutSessionBuilder aWorkoutSession(Faker faker) {
        return new WorkoutSessionBuilder(faker);
    }

    public WorkoutSessionBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public WorkoutSessionBuilder withStatus(SessionStatus status) {
        this.status = status;
        return this;
    }

    public WorkoutSessionBuilder withStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public WorkoutSessionBuilder withCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
        return this;
    }

    public WorkoutSessionBuilder withNotes(String notes) {
        this.notes = notes;
        return this;
    }

    public WorkoutSessionBuilder forUser(User user) {
        this.user = user;
        return this;
    }

    public WorkoutSessionBuilder withRoutine(Routine routine) {
        this.routine = routine;
        return this;
    }

    public WorkoutSessionBuilder withExercises(List<SessionExercise> exercises) {
        this.exercises = exercises != null ? new ArrayList<>(exercises) : new ArrayList<>();
        return this;
    }

    public WorkoutSessionBuilder addExercise(SessionExercise exercise) {
        if (this.exercises == null) {
            this.exercises = new ArrayList<>();
        }
        this.exercises.add(exercise);
        return this;
    }

    public WorkoutSession build() {
        if (user == null) {
            throw new IllegalStateException("User must be set to WorkoutSession");
        }

        WorkoutSession session = WorkoutSession.builder()
                .id(id)
                .status(status)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .notes(notes)
                .user(user)
                .routine(routine)
                .exercises(exercises)
                .build();

        if (exercises != null) {
            exercises.forEach(ex -> ex.setWorkoutSession(session));
        }

        return session;
    }
}
