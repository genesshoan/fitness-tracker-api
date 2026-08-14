package dev.genesshoan.fitnesstrackerapi.workout.domain;

import dev.genesshoan.fitnesstrackerapi.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "session_sets",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_session_sets_session_exercise_id_set_number",
                    columnNames = {"session_exercise_id", "set_number"})
        })
public class SessionSet extends BaseEntity {

    @Column(nullable = false)
    private int setNumber;

    private Integer reps;

    private Double weightKg;

    private Integer durationSeconds;

    private Double distanceKm;

    @Builder.Default
    private boolean completed = false;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "session_exercise_id")
    private SessionExercise sessionExercise;
}
