package dev.genesshoan.fitnesstrackerapi.infrastructure.script.data;

import java.io.InputStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class SeedData {

    public final DatabaseSeed databaseSeed;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public SeedData() {
        try {
            databaseSeed = load("seeds/database_seed.yml", new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to load seed data", e);
        }
    }

    private <T> T load(String path, TypeReference<T> type) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);

        if (is == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }

        return mapper.readValue(is, type);
    }
}
