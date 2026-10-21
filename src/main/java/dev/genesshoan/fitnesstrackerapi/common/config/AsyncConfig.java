package dev.genesshoan.fitnesstrackerapi.common.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Configures how failures raised by {@code @Async} methods are reported.
 *
 * <p>An exception thrown inside an async task cannot reach the caller, so Spring
 * hands it to the {@link AsyncUncaughtExceptionHandler}. Without this handler the
 * default one only logs a generic warning, which hides failures such as a
 * notification email that could not be delivered.
 */
@Slf4j
@Configuration
public class AsyncConfig {

    /**
     * Creates the handler that logs uncaught failures of async tasks.
     *
     * @return the uncaught exception handler
     */
    @Bean
    AsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("Async task {}.{} failed", method.getDeclaringClass().getSimpleName(), method.getName(), ex);
    }
}
