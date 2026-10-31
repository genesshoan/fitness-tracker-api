package dev.genesshoan.fitnesstrackerapi.gamification.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import dev.genesshoan.fitnesstrackerapi.common.domain.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * The best value the user has reached for one exercise and one metric.
 *
 * <p>The metrics that are compared per weight, such as
 * {@link PersonalRecordMetric#MAX_REPS_AT_WEIGHT}, identify the weight they
 * belong to through {@link #weightKg}. The remaining metrics keep it
 * {@code null}.</p>
 *
 * <p>The references to the user, the exercise, and the training that produced
 * the record are plain identifiers on purpose. This module does not depend on
 * the entities of the other modules.</p>
 */
@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Table(name = "personal_records")
public class PersonalRecord extends BaseEntity {

    /**
     * The user this record belongs to.
     */
    @Column(nullable = false, updatable = false)
    private UUID userId;

    /**
     * The exercise the record was set on.
     */
    @Column(nullable = false, updatable = false)
    private UUID exerciseId;

    /**
     * The metric the record was set for.
     */
    @Column(nullable = false, updatable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private PersonalRecordMetric metric;

    /**
     * The weight the record belongs to, for the metrics that are compared per
     * weight. {@code null} for the remaining metrics.
     */
    @Column(updatable = false)
    private Double weightKg;

    /**
     * The value of the record. For
     * {@link PersonalRecordMetric#MAX_REPS_AT_WEIGHT} this is the number of
     * repetitions.
     */
    @Column(nullable = false)
    private Double value;

    /**
     * The set that set the record.
     */
    @Column(updatable = false)
    private UUID setId;

    /**
     * The workout session that set the record.
     */
    @Column(updatable = false)
    private UUID sessionId;

    /**
     * The instant the record was set.
     */
    @Column(nullable = false, updatable = false)
    private Instant achievedAt;
}
