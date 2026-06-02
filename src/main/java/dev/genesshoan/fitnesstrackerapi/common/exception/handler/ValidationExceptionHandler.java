package dev.genesshoan.fitnesstrackerapi.common.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

import static dev.genesshoan.fitnesstrackerapi.common.exception.handler.ProblemDetailUtils.*;

/**
 * Centralized validation exception handler for the REST API.
 *
 * Handles validation-related exceptions and transforms them into RFC 7807
 * Problem Detail responses with appropriate HTTP status codes and descriptive
 * error information. This handler focuses on bean validation, constraint violations,
 * and missing request parameters.
 *
 * Processing order: This handler is ordered at priority 1, making it the first
 * handler evaluated for validation-related exceptions, ensuring validation errors
 * are caught before other exception handlers.
 *
 * Supported exceptions:
 * - {@link MethodArgumentNotValidException}: Request body validation failures
 * - {@link ConstraintViolationException}: Constraint violations on parameters/path variables
 * - {@link MissingServletRequestParameterException}: Missing required request parameters
 *
 * Error details are grouped by field/parameter name to provide structured, actionable
 * validation error information to API clients.
 *
 * @see PersistenceExceptionHandler for database-related exceptions
 * @see HttpExceptionHandler for HTTP-level exceptions
 */
@Slf4j
@Order(1)
@RestControllerAdvice
public class ValidationExceptionHandler {

    /**
     * Handles validation errors thrown when a request body annotated with {@code @Valid}
     * fails Bean Validation (Jakarta Validation / Hibernate Validator) constraints.
     *
     * Collects all field validation errors and groups them by field name to provide
     * detailed feedback on which fields failed validation and why.
     *
     * @param ex the exception containing field and global validation errors
     * @param request the current HTTP request
     * @return a {@link ResponseEntity} with 400 Bad Request status and a grouped map
     *         of validation error details per field
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.warn("Validation failed: {} {} -> {} fields with errors",
                request.getMethod(),
                request.getRequestURI(),
                ex.getFieldErrorCount());

        Map<String, List<String>> errors = groupErrors(
                ex.getFieldErrors(),
                FieldError::getField,
                FieldError::getDefaultMessage);

        ProblemDetail problemDetail = errorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more fields are invalid",
                errors,
                request
        );

        return ResponseEntity.badRequest()
                .body(problemDetail);
    }

    /**
     * Handles {@link ConstraintViolationException} thrown when validation of request
     * parameters, path variables, or method arguments fails during constraint evaluation.
     *
     * This is typically triggered by {@code @Validated} annotations on controller parameters
     * or when using method-level validation on service classes. Groups constraint violations
     * by property path to provide clear feedback on which parameters are invalid.
     *
     * @param ex the exception containing the set of violated constraints
     * @param request the current HTTP request
     * @return a {@link ResponseEntity} with 400 Bad Request status and a grouped map
     *         of constraint violation details per property/parameter
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {

        log.warn("Constraint violation: {} {} -> {} violations",
                request.getMethod(),
                request.getRequestURI(),
                ex.getConstraintViolations().size());

        Map<String, List<String>> errors = groupErrors(
                ex.getConstraintViolations(),
                violation -> violation.getPropertyPath().toString(),
                ConstraintViolation::getMessage
        );

        ProblemDetail problemDetail = errorResponse(
                HttpStatus.BAD_REQUEST,
                "Constraint violation",
                "One or more request parameters violate validation constraints",
                errors,
                request
        );

        return ResponseEntity.badRequest()
                .body(problemDetail);
    }

    /**
     * Handles {@link MissingServletRequestParameterException} thrown when a required
     * request parameter (query parameter or form parameter) is missing.
     *
     * Provides clear feedback on which parameter name is required, allowing clients
     * to correct their requests by including the missing parameter.
     *
     * @param ex the exception containing details about the missing parameter
     *           (name, type, etc.)
     * @param request the current HTTP request
     * @return a {@link ResponseEntity} with 400 Bad Request status and details
     *         about the missing parameter
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {

        log.warn("Missing parameter: {} {} -> parameter '{}' is required",
                request.getMethod(),
                request.getRequestURI(),
                ex.getParameterName());

        ProblemDetail problemDetail = errorResponse(
                HttpStatus.BAD_REQUEST,
                "Missing request parameter",
                String.format("Required parameter '%s' is missing", ex.getParameterName()),
                null,
                request
        );

        return ResponseEntity.badRequest()
                .body(problemDetail);
    }
}
