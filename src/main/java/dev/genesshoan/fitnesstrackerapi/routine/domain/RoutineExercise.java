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

/**
 * An exercise assignment within a routine, including its position and default
 * training metrics.
 */
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

    /** The unique identifier of this routine exercise assignment. */
    @Id
    @Column(updatable = false, nullable = false, unique = true)
    UUID id;

    /** One-based position of the exercise within its routine. */
    @Column(nullable = false)
    private Integer position;

    /** Default rest interval between sets, in seconds. */
    @Column(nullable = false)
    private Integer defaultRestSeconds;

    /** Default number of sets. */
    @Column(nullable = false)
    private Integer defaultSets;

    /** Default repetitions per set, when applicable to the exercise category. */
    @Column
    private Integer defaultReps;

    /** Default weight in kilograms, when applicable to the exercise category. */
    @Column
    private Double defaultWeightKg;

    /** Default duration in seconds, when applicable to the exercise category. */
    @Column
    private Integer defaultDurationSeconds;

    /** Default distance in kilometers, when applicable to the exercise category. */
    @Column
    private Double defaultDistanceKm;

    /** Optional notes for performing this exercise in the routine. */
    @Column
    private String notes;

    /** The routine containing this exercise assignment. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id")
    private Routine routine;

    /** The catalog exercise referenced by this assignment. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;
}
