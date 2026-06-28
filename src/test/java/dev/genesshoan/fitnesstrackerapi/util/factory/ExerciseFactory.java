package dev.genesshoan.fitnesstrackerapi.util.factory;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseDetailDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseListItemDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseMuscleDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.BodyRegion;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.dto.MuscleResponseDTO;
import java.util.List;
import java.util.UUID;

public final class ExerciseFactory {

    private ExerciseFactory() {}

    public static Exercise exercise() {
        return Exercise.builder()
            .id(UUID.randomUUID())
            .name("Bench Press")
            .slug("bench-press")
            .description("Bench press description")
            .category(Category.STRENGTH)
            .difficulty(Difficulty.BEGINNER)
            .build();
    }

    public static Exercise exercise(UUID id) {
        return Exercise.builder()
            .id(id)
            .name("Bench Press")
            .slug("bench-press")
            .description("Bench press description")
            .category(Category.STRENGTH)
            .difficulty(Difficulty.BEGINNER)
            .build();
    }

    public static ExerciseListItemDTO listItem() {
        return new ExerciseListItemDTO(
            UUID.randomUUID(),
            "Bench Press",
            "bench-press",
            Category.STRENGTH,
            Difficulty.BEGINNER
        );
    }

    public static ExerciseListItemDTO listItem(UUID id) {
        return new ExerciseListItemDTO(
            id,
            "Bench Press",
            "bench-press",
            Category.STRENGTH,
            Difficulty.BEGINNER
        );
    }

    public static ExerciseDetailDTO detail() {
        return new ExerciseDetailDTO(
            UUID.randomUUID(),
            "Bench Press",
            "bench-press",
            "Bench press description",
            Category.STRENGTH,
            Difficulty.BEGINNER,
            List.of(
                new ExerciseMuscleDTO(
                    new MuscleResponseDTO(
                        "Pectoralis Major",
                        "pectoralis-major",
                        BodyRegion.CHEST
                    ),
                    ImpactLevel.PRIMARY
                )
            )
        );
    }
}
