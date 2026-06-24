package dev.genesshoan.fitnesstrackerapi.common.script;

import com.github.f4b6a3.uuid.UuidCreator;
import dev.genesshoan.fitnesstrackerapi.common.script.data.*;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class SeedGenerator {

    private final StringBuilder sql = new StringBuilder();

    private final Map<String, UUID> muscleIds = new HashMap<>();
    private final Map<String, UUID> exerciseIds = new HashMap<>();

    private final SeedData seedData = new SeedData();

    public static void main(String[] args) {
        new SeedGenerator().generate();
    }

    public void generate() {
        generateMuscles();
        generateExercises();
        generateExerciseMuscles();

        writeToFile();

        System.out.println(sql);
    }

    private void writeToFile() {
        try {
            Path output = Path.of(
                "src/main/resources/db/migration/V6__seed_exercise_muscle_data.sql"
            );

            Files.createDirectories(output.getParent());

            Files.writeString(
                output,
                sql.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );

            System.out.println(
                "Flyway migration generated at: " + output.toAbsolutePath()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to write Flyway migration", e);
        }
    }

    private void generateMuscles() {
        sql.append("-- MUSCLES\n");
        sql.append(
            "INSERT INTO muscles (id, name, slug, body_region) VALUES\n"
        );

        List<String> rows = new ArrayList<>();

        for (MuscleSeed muscle : seedData.muscles) {
            UUID id = newId();
            muscleIds.put(muscle.slug(), id);

            rows.add(row(id, muscle.name(), muscle.slug(), muscle.region()));
        }

        sql.append(String.join(",\n", rows));
        sql.append(";\n\n");
    }

    private void generateExercises() {
        sql.append("-- EXERCISES\n");
        sql.append(
            "INSERT INTO exercises (id, name, slug, description, category, difficulty) VALUES\n"
        );

        List<String> rows = new ArrayList<>();

        for (ExerciseSeed exercise : seedData.exercises) {
            UUID id = newId();
            exerciseIds.put(exercise.slug(), id);

            rows.add(
                row(
                    id,
                    exercise.name(),
                    exercise.slug(),
                    exercise.description(),
                    exercise.category(),
                    exercise.difficulty()
                )
            );
        }

        sql.append(String.join(",\n", rows));
        sql.append(";\n\n");
    }

    private void generateExerciseMuscles() {
        sql.append("-- EXERCISE MUSCLES\n");
        sql.append(
            "INSERT INTO exercise_muscles (exercise_id, muscle_id, impact_level) VALUES\n"
        );

        List<String> rows = new ArrayList<>();

        for (ExerciseSeed exercise : seedData.exercises) {
            String exerciseSlug = exercise.slug();

            addMuscleRows(
                rows,
                exerciseSlug,
                exercise.muscles().primary(),
                ImpactLevel.PRIMARY
            );
            addMuscleRows(
                rows,
                exerciseSlug,
                exercise.muscles().secondary(),
                ImpactLevel.SECONDARY
            );
            addMuscleRows(
                rows,
                exerciseSlug,
                exercise.muscles().stabilizer(),
                ImpactLevel.STABILIZER
            );
        }

        sql.append(String.join(",\n", rows));
        sql.append(";\n\n");
    }

    private void addMuscleRows(
        List<String> rows,
        String exerciseSlug,
        List<String> muscleSlugs,
        ImpactLevel impact
    ) {
        if (muscleSlugs == null) return;

        for (String muscleSlug : muscleSlugs) {
            UUID exerciseId = exerciseIds.get(exerciseSlug);
            UUID muscleId = muscleIds.get(muscleSlug);

            if (exerciseId == null) {
                throw new IllegalStateException(
                    "Missing exercise slug: " + exerciseSlug
                );
            }

            if (muscleId == null) {
                throw new IllegalStateException(
                    "Missing muscle slug: " + muscleSlug
                );
            }

            rows.add(row(exerciseId, muscleId, impact.name()));
        }
    }

    private UUID newId() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    private String row(Object... values) {
        StringBuilder sb = new StringBuilder("(");

        for (int i = 0; i < values.length; i++) {
            sb.append("'").append(escape(values[i])).append("'");

            if (i < values.length - 1) {
                sb.append(", ");
            }
        }

        sb.append(")");
        return sb.toString();
    }

    private String escape(Object value) {
        if (value == null) return "";

        return value.toString().replace("'", "''");
    }
}
