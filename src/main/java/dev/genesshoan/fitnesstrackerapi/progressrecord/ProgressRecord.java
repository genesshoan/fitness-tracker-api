package dev.genesshoan.fitnesstrackerapi.progressrecord;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import dev.genesshoan.fitnesstrackerapi.common.BaseEntity;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "progress_records", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "recorded_at"}))
public class ProgressRecord extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private LocalDate recordedAt;

    @Column(nullable = false, updatable = false)
    private Double weightKg;

    private Double bodyFatPercentage;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
