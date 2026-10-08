package dev.genesshoan.fitnesstrackerapi.exercise.domain.muscle;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import dev.genesshoan.fitnesstrackerapi.common.domain.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Table(name = "muscle_assets")
public class MuscleAsset extends BaseEntity {

    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;

    @Column(nullable = false)
    private String variant;

    @Column(nullable = false)
    private String view;

    @Column(name = "content_type")
    private String contentType;

    @ManyToMany(mappedBy = "assets", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Muscle> muscles = new HashSet<>();
}
