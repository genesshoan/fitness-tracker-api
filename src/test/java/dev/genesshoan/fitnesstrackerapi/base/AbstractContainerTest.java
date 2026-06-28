package dev.genesshoan.fitnesstrackerapi.base;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

@ActiveProfiles("test")
public abstract class AbstractContainerTest {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:18-alpine").withReuse(true);

    static {
        POSTGRES.start();
    }
}
