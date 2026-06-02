package dev.genesshoan.fitnesstrackerapi.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static dev.genesshoan.fitnesstrackerapi.error.ProblemDetailUtils.errorResponse;

/**
 * Global catch-all exception handler for unexpected errors.
 *
 * This handler is the fallback for any exceptions that are not caught by more specific
 * exception handlers (ValidationExceptionHandler, HttpExceptionHandler, PersistenceExceptionHandler).
 * It serves as a safety net to ensure all unhandled exceptions result in proper RFC 7807
 * Problem Detail responses instead of generic server errors.
 *
 * Processing order: This handler is ordered at {@link Ordered#LOWEST_PRECEDENCE}, making it
 * the last handler to be evaluated. This ensures all more specific handlers get a chance to
 * process exceptions before this catch-all fallback is invoked.
 *
 * Security and environment awareness:
 * - In development profile ("dev"): Full exception messages are exposed to aid debugging
 * - In production profiles: Generic error messages are shown to prevent information disclosure
 *
 * Note: This class is NOT annotated with {@code @RestControllerAdvice} (unlike the other
 * exception handlers). To enable this handler, either:
 * 1. Add {@code @RestControllerAdvice} annotation to this class, or
 * 2. Register it as a bean in the Spring context
 *
 * @see ValidationExceptionHandler for validation-related exceptions (Order 1)
 * @see HttpExceptionHandler for HTTP protocol exceptions (Order 2)
 * @see PersistenceExceptionHandler for database exceptions (Order 3)
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * The active Spring profile(s). Injected from {@code spring.profiles.active} property.
     * Used to determine whether to expose detailed error messages (dev) or generic ones (production).
     */
    @Value("${spring.profiles.active}")
    private String activeProfile;

    /**
     * Handles any unexpected exception that was not caught by specific exception handlers.
     *
     * This is the ultimate fallback handler. It logs the full exception with stack trace
     * for debugging purposes, but the error message returned to the client depends on
     * the active Spring profile:
     * - Development: Exposes the exception message for debugging
     * - Production: Returns a generic message to prevent information disclosure vulnerabilities
     *
     * @param ex the unexpected exception that occurred
     * @param request the current HTTP request that triggered the exception
     * @return a {@link ResponseEntity} with 500 Internal Server Error status and appropriate
     *         error message based on the active profile
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error at {} {}", request.getMethod(), request.getRequestURI(), ex);

        String detail = "dev".equals(activeProfile) ? ex.getMessage() : "An unexpected error occurred";

        ProblemDetail problemDetail = errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                detail,
                null,
                request);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problemDetail);
    }
}
