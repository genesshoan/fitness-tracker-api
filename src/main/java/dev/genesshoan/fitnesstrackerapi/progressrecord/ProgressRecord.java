package dev.genesshoan.fitnesstrackerapi.progressrecord;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import dev.genesshoan.fitnesstrackerapi.common.domain.BaseEntity;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Represents a user's body measurement snapshot at a specific date.
 *
 * <p>Each progress record captures the user's weight and optional body fat percentage
 * on a given date. The combination of user_id and recorded_at is unique, ensuring
 * only one measurement per day per user.
 *
 * <p>Fields are immutable after creation (updatable = false).
 */
@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "progress_records", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "recorded_at"}))
public class ProgressRecord extends BaseEntity {

    /**
     * The date when the measurement was recorded.
     * Immutable after creation.
     */
    @Column(nullable = false, updatable = false)
    private LocalDate recordedAt;

    /**
     * User weight in kilograms.
     * Immutable after creation.
     */
    @Column(nullable = false, updatable = false)
    private Double weightKg;

    /**
     * Optional body fat percentage (0-100).
     */
    private Double bodyFatPercentage;

    /**
     * The user who owns this progress record.
     * Lazy-loaded; must be initialized within an active persistence context.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
