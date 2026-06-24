package dev.genesshoan.fitnesstrackerapi.common.script.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;
import java.util.Set;

public class SeedData {

    public final Set<MuscleSeed> muscles;
    public final Set<ExerciseSeed> exercises;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public SeedData() {
        try {
            muscles = load("seeds/muscles.yml", new TypeReference<>() {});
            exercises = load("seeds/exercises.yml", new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to load seed data", e);
        }
    }

    private <T> Set<T> load(String path, TypeReference<Set<T>> type)
        throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);

        if (is == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }

        return mapper.readValue(is, type);
    }
}
