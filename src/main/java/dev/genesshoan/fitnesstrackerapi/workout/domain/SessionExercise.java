package dev.genesshoan.fitnesstrackerapi.workout.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import dev.genesshoan.fitnesstrackerapi.common.domain.BaseEntity;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
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
@Table(
        name = "session_exercises",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_session_exercises_session_id_position",
                    columnNames = {"session_id", "position"})
        })
public class SessionExercise extends BaseEntity {

    @Column(nullable = false)
    private int position;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private WorkoutSession workoutSession;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @Builder.Default
    @OneToMany(mappedBy = "sessionExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("setNumber ASC")
    private List<SessionSet> sets = new ArrayList<>();

    public void addSet(SessionSet set) {
        sets.add(set);
        set.setSessionExercise(this);
    }

    public void removeSet(SessionSet set) {
        sets.remove(set);
        set.setSessionExercise(null);
    }
}
