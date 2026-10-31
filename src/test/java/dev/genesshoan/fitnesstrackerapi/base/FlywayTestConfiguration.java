package dev.genesshoan.fitnesstrackerapi.base;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Makes the Flyway migrations the single source of truth for the test schema.
 *
 * <p>The test profile used to run with {@code ddl-auto: create-drop}. Hibernate
 * dropped the migrated schema and rebuilt it from the entity mapping, so the
 * migrations were never exercised and a column that drifted between a migration
 * and its entity stayed invisible.
 *
 * <p>The schema is now migrated by Flyway and validated by Hibernate, which means
 * the suite runs against the same schema, foreign keys, check constraints, and
 * indexes that production has.
 *
 * <p>The seeded exercise and muscle catalog is cleared afterwards. It is
 * reference data that production ships with, but the tests build their own
 * catalog and assert on exact contents, so leaving it in place would make those
 * assertions depend on the seed instead of on the behavior under test.
 */
@TestConfiguration
public class FlywayTestConfiguration {

    private static final String CLEAR_SEEDED_CATALOG = """
            TRUNCATE TABLE exercises, muscles, exercise_muscles, muscle_assets, muscle_asset_mappings CASCADE
            """;

    /**
     * Migrates the schema and then clears the seeded catalog.
     *
     * <p>The data source is taken from the Flyway configuration instead of being
     * injected. Injecting a {@link JdbcTemplate} would create a circular
     * dependency, because the template needs a data source whose creation is
     * already waiting on the Flyway initializer.
     */
    @Bean
    FlywayMigrationStrategy migrateAndClearSeededCatalog() {
        return flyway -> {
            flyway.migrate();
            DataSource dataSource = (DataSource) flyway.getConfiguration().getDataSource();
            new JdbcTemplate(dataSource).execute(CLEAR_SEEDED_CATALOG);
        };
    }
}
