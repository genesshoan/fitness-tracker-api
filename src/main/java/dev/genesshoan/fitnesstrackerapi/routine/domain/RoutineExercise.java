package dev.genesshoan.fitnesstrackerapi.routine.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "routine_exercises",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_routine_exercise_position",
                        columnNames = {"routine_id", "position"}))
public class RoutineExercise {

    @Id
    @Column(updatable = false, nullable = false, unique = true)
    UUID id;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false)
    private Integer defaultRestSeconds;

    @Column(nullable = false)
    private Integer defaultSets;

    @Column
    private Integer defaultReps;

    @Column
    private Double defaultWeightKg;

    @Column
    private Integer defaultDurationSeconds;

    @Column
    private Double defaultDistanceKm;

    @Column
    private String notes;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id")
    private Routine routine;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;
}
