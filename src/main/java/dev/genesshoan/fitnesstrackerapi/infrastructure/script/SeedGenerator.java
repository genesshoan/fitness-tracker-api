package dev.genesshoan.fitnesstrackerapi.infrastructure.script;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import com.github.f4b6a3.uuid.UuidCreator;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.ImpactLevel;
import dev.genesshoan.fitnesstrackerapi.infrastructure.script.data.*;

public class SeedGenerator {

    private final StringBuilder sql = new StringBuilder();

    private final Map<String, UUID> muscleIds = new HashMap<>();
    private final Map<String, UUID> exerciseIds = new HashMap<>();
    private final Map<String, UUID> muscleAssetIds = new HashMap<>();

    private final SeedData seedData = new SeedData();

    public static void main(String[] args) {
        new SeedGenerator().generate();
    }

    public void generate() {
        generateMuscles();
        generateExercises();
        generateMuscleAssets();
        generateExerciseMuscles();
        generateMuscleAssetMappings();

        writeToFile();

        System.out.println(sql);
    }

    private void writeToFile() {
        try {
            Path output = Path.of("src/main/resources/db/migration/V6__seed_exercise_muscle_data.sql");

            Files.createDirectories(output.getParent());

            Files.writeString(output, sql.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("Flyway migration generated at: " + output.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to write Flyway migration", e);
        }
    }

    private void generateMuscles() {
        List<String> rows = new ArrayList<>();

        for (MuscleSeed muscle : seedData.databaseSeed.muscles()) {
            UUID id = newId();
            muscleIds.put(muscle.slug(), id);

            rows.add(row(id, muscle.name(), muscle.slug(), muscle.region()));
        }

        appendInsert("-- MUSCLES", "muscles (id, name, slug, body_region)", rows);
    }

    private void generateExercises() {
        List<String> rows = new ArrayList<>();

        for (ExerciseSeed exercise : seedData.databaseSeed.exercises()) {
            UUID id = newId();
            exerciseIds.put(exercise.slug(), id);

            rows.add(row(
                    id,
                    exercise.name(),
                    exercise.slug(),
                    exercise.description(),
                    exercise.category(),
                    exercise.difficulty(),
                    exercise.instructions(),
                    exercise.mediaObjectKey(),
                    exercise.thumbnailObjectKey()));
        }

        appendInsert(
                "-- EXERCISES",
                "exercises (id, name, slug, description, category, difficulty, instructions, media_object_key,"
                        + " thumbnail_object_key)",
                rows);
    }

    private void generateMuscleAssets() {
        List<String> rows = new ArrayList<>();
        Map<String, MuscleAssetSeed> assetsByKey = new LinkedHashMap<>();

        addAssets(assetsByKey, seedData.databaseSeed.muscleAssets());
        for (MuscleSeed muscle : seedData.databaseSeed.muscles()) {
            addAssets(assetsByKey, muscle.assets());
        }

        for (Map.Entry<String, MuscleAssetSeed> entry : assetsByKey.entrySet()) {
            UUID id = newId();
            muscleAssetIds.put(entry.getKey(), id);
            MuscleAssetSeed asset = entry.getValue();
            rows.add(row(id, asset.objectKey(), asset.variant(), asset.view(), asset.contentType()));
        }

        appendInsert("-- MUSCLE ASSETS", "muscle_assets (id, object_key, variant, view, content_type)", rows);
    }

    private void addAssets(Map<String, MuscleAssetSeed> assetsByKey, List<MuscleAssetSeed> assets) {
        if (assets == null) {
            return;
        }

        for (MuscleAssetSeed asset : assets) {
            if (asset == null || asset.objectKey() == null || asset.objectKey().isBlank()) {
                throw new IllegalStateException("Muscle asset must have an object_key");
            }

            MuscleAssetSeed previous = assetsByKey.putIfAbsent(asset.objectKey(), asset);
            if (previous != null
                    && (!Objects.equals(previous.variant(), asset.variant())
                            || !Objects.equals(previous.view(), asset.view())
                            || !Objects.equals(previous.contentType(), asset.contentType()))) {
                throw new IllegalStateException(
                        "Conflicting muscle asset metadata for object_key: " + asset.objectKey());
            }
        }
    }

    private void generateExerciseMuscles() {
        List<String> rows = new ArrayList<>();

        for (ExerciseSeed exercise : seedData.databaseSeed.exercises()) {
            String exerciseSlug = exercise.slug();

            addMuscleRows(rows, exerciseSlug, exercise.muscles().primary(), ImpactLevel.PRIMARY);
            addMuscleRows(rows, exerciseSlug, exercise.muscles().secondary(), ImpactLevel.SECONDARY);
            addMuscleRows(rows, exerciseSlug, exercise.muscles().stabilizer(), ImpactLevel.STABILIZER);
        }

        appendInsert("-- EXERCISE MUSCLES", "exercise_muscles (exercise_id, muscle_id, impact_level)", rows);
    }

    private void generateMuscleAssetMappings() {
        List<String> rows = new ArrayList<>();
        Set<String> mappingKeys = new HashSet<>();
        List<MuscleAssetMappingSeed> mappings = seedData.databaseSeed.muscleAssetMappings();

        if (mappings != null) {
            for (MuscleAssetMappingSeed mapping : mappings) {
                UUID muscleId = muscleIds.get(mapping.muscleSlug());
                UUID assetId = muscleAssetIds.get(mapping.objectKey());

                if (muscleId == null) {
                    throw new IllegalStateException("Missing muscle slug: " + mapping.muscleSlug());
                }
                if (assetId == null) {
                    throw new IllegalStateException("Missing muscle asset object_key: " + mapping.objectKey());
                }

                if (mappingKeys.add(mapping.muscleSlug() + "\u0000" + mapping.objectKey())) {
                    rows.add(row(muscleId, assetId));
                }
            }
        }

        appendInsert("-- MUSCLE ASSET MAPPINGS", "muscle_asset_mappings (muscle_id, muscle_asset_id)", rows);
    }

    private void appendInsert(String comment, String columns, List<String> rows) {
        if (rows.isEmpty()) {
            return;
        }

        sql.append(comment).append("\n");
        sql.append("INSERT INTO ").append(columns).append(" VALUES\n");
        sql.append(String.join(",\n", rows));
        sql.append(";\n\n");
    }

    private void addMuscleRows(List<String> rows, String exerciseSlug, List<String> muscleSlugs, ImpactLevel impact) {
        if (muscleSlugs == null) return;

        for (String muscleSlug : muscleSlugs) {
            UUID exerciseId = exerciseIds.get(exerciseSlug);
            UUID muscleId = muscleIds.get(muscleSlug);

            if (exerciseId == null) {
                throw new IllegalStateException("Missing exercise slug: " + exerciseSlug);
            }

            if (muscleId == null) {
                throw new IllegalStateException("Missing muscle slug: " + muscleSlug);
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
            Object value = values[i];

            if (value == null) {
                sb.append("NULL");
            } else if (value instanceof List<?> list) {
                sb.append(formatArray(list));
            } else {
                sb.append("'").append(escape(value)).append("'");
            }

            if (i < values.length - 1) {
                sb.append(", ");
            }
        }

        sb.append(")");
        return sb.toString();
    }

    private String formatArray(List<?> values) {
        if (values == null || values.isEmpty()) {
            return "ARRAY[]::TEXT[]";
        }

        StringBuilder sb = new StringBuilder("ARRAY[");

        for (int i = 0; i < values.size(); i++) {
            sb.append("'").append(escape(values.get(i))).append("'");

            if (i < values.size() - 1) {
                sb.append(", ");
            }
        }

        sb.append("]");
        return sb.toString();
    }

    private String escape(Object value) {
        if (value == null) return "";

        return value.toString().replace("'", "''");
    }
}
