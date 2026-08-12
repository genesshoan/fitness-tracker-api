package dev.genesshoan.fitnesstrackerapi.workout.domain;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import dev.genesshoan.fitnesstrackerapi.common.BaseEntity;
import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
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
import jakarta.persistence.Version;
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

    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Version
    private int version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id")
    private Routine routine;

    @OneToMany(
        mappedBy = "workoutSession",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )

    @Builder.Default
    @OrderBy("position ASC")
    private List<SessionExercise> exercises = new ArrayList<>();
}
