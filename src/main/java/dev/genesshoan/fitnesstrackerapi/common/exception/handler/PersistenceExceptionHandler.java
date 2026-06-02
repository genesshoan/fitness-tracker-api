package dev.genesshoan.fitnesstrackerapi.common.exception.handler;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static dev.genesshoan.fitnesstrackerapi.common.exception.handler.ProblemDetailUtils.errorResponse;


/**
 * Exception handler for persistence and database-related exceptions.
 *
 * This handler is responsible for catching and transforming exceptions that occur
 * during database operations (e.g., JPA, Hibernate, SQL-related errors) into
 * appropriate HTTP responses with RFC 7807 Problem Detail format.
 *
 * Processing order: This handler is ordered at priority 3, meaning it will be
 * evaluated after handlers with lower order values but before higher ones.
 *
 * Common exceptions handled:
 * - JPA/Hibernate exceptions (EntityNotFoundException, DataIntegrityViolationException, etc.)
 * - Database constraint violations (unique constraints, foreign keys)
 * - Transaction-related errors
 *
 * @see HttpExceptionHandler for HTTP-level exception handling
 */
@Slf4j
@Order(3)
@RestControllerAdvice
public class PersistenceExceptionHandler {

    /**
     * Handles database constraint violation exceptions.
     *
     * Catches exceptions thrown when database integrity constraints are violated,
     * such as unique key violations, foreign key constraint failures, or other
     * database-level constraint errors.
     *
     * @param ex the exception thrown when a constraint is violated
     * @param request the HTTP request
     * @return 409 Conflict with details about the constraint violation
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {

        log.warn("Data integrity violation: {} {} -> {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());

        ProblemDetail problemDetail = errorResponse(
                HttpStatus.CONFLICT,
                "Data integrity violation",
                "The request violates database constraints",
                null,
                request);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    /**
     * Handles missing entity exceptions.
     *
     * Catches exceptions thrown when attempting to access or retrieve an entity
     * that does not exist in the database. This typically occurs when an entity
     * is queried by ID or other lookup criteria and no matching record is found.
     *
     * @param ex the exception thrown when an entity is not found
     * @param request the HTTP request
     * @return 404 Not Found with error details
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFoundException(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {

        log.warn("Entity not found: {} {} -> {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

        ProblemDetail problemDetail = errorResponse(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                "The requested resource was not found",
                null,
                request
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }
}
