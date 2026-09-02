package dev.genesshoan.fitnesstrackerapi.workout.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import dev.genesshoan.fitnesstrackerapi.common.domain.BaseEntity;
import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@Table(name = "workout_sessions")
public class WorkoutSession extends BaseEntity {

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    private Instant completedAt;

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id")
    private Routine routine;

    @OneToMany(mappedBy = "workoutSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("position ASC")
    private List<SessionExercise> exercises = new ArrayList<>();

    public void finish() {
        this.status = SessionStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void addExerciseAt(SessionExercise sessionExercise, Integer requestedPosition) {
        int resolvedPosition = resolvePosition(requestedPosition, exercises.size() + 1);

        shiftExercisesFromPosition(resolvedPosition);
        sessionExercise.setPosition(resolvedPosition);
        addExercise(sessionExercise);
    }

    public void removeExercise(SessionExercise exercise) {
        int deletedPosition = exercise.getPosition();

        exercises.remove(exercise);
        exercise.setWorkoutSession(null);

        closePositionGap(deletedPosition);
    }

    public void moveExercise(SessionExercise sessionExercise, int requestedPosition) {
        int newPosition = resolvePosition(requestedPosition, exercises.size());
        int oldPosition = sessionExercise.getPosition();

        if (oldPosition == newPosition) {
            return;
        }

        exercises.forEach(other -> {
            if (other == sessionExercise) {
                return;
            }

            if (newPosition < oldPosition && other.getPosition() >= newPosition && other.getPosition() < oldPosition) {
                other.setPosition(other.getPosition() + 1);
            } else if (newPosition > oldPosition
                    && other.getPosition() > oldPosition
                    && other.getPosition() <= newPosition) {
                other.setPosition(other.getPosition() - 1);
            }
        });

        sessionExercise.setPosition(newPosition);
    }

    public Optional<SessionExercise> findExercise(UUID exerciseId) {
        return exercises.stream().filter(se -> se.getId().equals(exerciseId)).findFirst();
    }

    private void addExercise(SessionExercise sessionExercise) {
        exercises.add(sessionExercise);
        sessionExercise.setWorkoutSession(this);
    }

    private void closePositionGap(int deletedPosition) {
        exercises.stream()
                .filter(se -> se.getPosition() >= deletedPosition)
                .forEach(se -> se.setPosition(se.getPosition() - 1));
    }

    private void shiftExercisesFromPosition(int position) {
        exercises.stream()
                .filter(exercise -> exercise.getPosition() >= position)
                .forEach(exercise -> exercise.setPosition(exercise.getPosition() + 1));
    }

    private int resolvePosition(Integer requestedPosition, int maxPosition) {
        if (requestedPosition == null) {
            return maxPosition;
        }

        return Math.max(1, Math.min(requestedPosition, maxPosition));
    }
}
