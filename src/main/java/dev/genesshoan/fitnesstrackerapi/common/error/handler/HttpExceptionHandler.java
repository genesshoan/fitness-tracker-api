package dev.genesshoan.fitnesstrackerapi.common.error.handler;

import static dev.genesshoan.fitnesstrackerapi.common.error.handler.ProblemDetailUtils.errorResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Centralized HTTP exception handler for the REST API.
 *
 * Maps common Spring and HTTP-level exceptions to RFC 7807 Problem Detail responses
 * with appropriate HTTP status codes and descriptive error messages. This handler
 * focuses on HTTP protocol-level errors and request parsing failures.
 *
 * Processing order: This handler is ordered at priority 2, meaning it is evaluated
 * after the ValidationExceptionHandler (Order 1) but before the PersistenceExceptionHandler
 * (Order 3), ensuring HTTP errors are caught in the correct sequence.
 *
 * Supported exceptions:
 * - {@link HttpMessageNotReadableException}: Malformed JSON request bodies
 * - {@link MethodArgumentTypeMismatchException}: Type mismatches in parameters/path variables
 * - {@link NoHandlerFoundException}: Requests to non-existent endpoints
 * - {@link HttpRequestMethodNotSupportedException}: Unsupported HTTP methods (PUT, DELETE, etc.)
 * - {@link HttpMediaTypeNotSupportedException}: Unsupported content types (e.g., XML when expecting JSON)
 *
 * @see ValidationExceptionHandler for request validation exceptions
 * @see PersistenceExceptionHandler for database-related exceptions
 */
@Slf4j
@Order(2)
@RestControllerAdvice
public class HttpExceptionHandler {

    /**
     * Handles malformed JSON request bodies.
     *
     * This exception is typically thrown by Spring's JSON message converter when
     * the request body cannot be parsed as valid JSON. Common causes include syntax
     * errors, unclosed braces/brackets, and invalid escape sequences.
     *
     * @param ex the exception thrown during JSON parsing, containing details about
     *           the parsing error location and cause
     * @param request the current HTTP request
     * @return a {@link ResponseEntity} with 400 Bad Request status and error details
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Malformed JSON body: {} {} -> {}",
            request.getMethod(),
            request.getRequestURI(),
            ex.getMessage()
        );

        ProblemDetail problemDetail = errorResponse(
            HttpStatus.BAD_REQUEST,
            "Malformed JSON body",
            "The request contains invalid JSON content",
            null,
            request
        );

        return ResponseEntity.badRequest().body(problemDetail);
    }

    /**
     * Handles type mismatches in request parameters or path variables.
     *
     * This exception occurs when Spring attempts to convert a parameter value to
     * its target type but fails. For example, attempting to pass "abc" when an
     * integer ID is expected, or passing an invalid UUID format.
     *
     * @param ex the exception containing details about the invalid parameter,
     *           including the received value, parameter name, and expected type
     * @param request the current HTTP request
     * @return a {@link ResponseEntity} with 400 Bad Request status, including
     *         the invalid value, parameter name, and expected type
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Invalid parameter type: {} {} -> {}",
            request.getMethod(),
            request.getRequestURI(),
            ex.getMessage()
        );

        String detail = String.format(
            "Invalid value '%s' for parameter '%s'. Expected type: %s",
            ex.getValue(),
            ex.getName(),
            ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "unknown"
        );

        ProblemDetail problemDetail = errorResponse(
            HttpStatus.BAD_REQUEST,
            "Invalid parameter type",
            detail,
            null,
            request
        );

        return ResponseEntity.badRequest().body(problemDetail);
    }

    /**
     * Handles requests to non-existent endpoints.
     *
     * This exception is thrown by Spring's DispatcherServlet when no handler mapping
     * can be found for the requested path and HTTP method combination. Requires
     * {@code spring.mvc.throw-exception-if-no-handler-found=true} and
     * {@code spring.resources.add-mappings=false} in application configuration.
     *
     * @param ex the exception containing the HTTP method and request URL that
     *           triggered the 404
     * @param request the current HTTP request
     * @return a {@link ResponseEntity} with 404 Not Found status and error details
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoHandlerFoundException(
        NoHandlerFoundException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "No handler found: {} {} (resource not available)",
            request.getMethod(),
            request.getRequestURI()
        );

        ProblemDetail problemDetail = errorResponse(
            HttpStatus.NOT_FOUND,
            "Resource not found",
            "The requested endpoint does not exist",
            null,
            request
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Handles HTTP methods not supported by an endpoint.
     *
     * This exception occurs when a client attempts to use an HTTP method (e.g., DELETE, PUT)
     * on an endpoint that only supports other methods (e.g., GET, POST). Spring throws this
     * when the DispatcherServlet finds a matching path but no handler for the given HTTP method.
     *
     * @param ex the exception containing the unsupported HTTP method and allowed methods
     * @param request the current HTTP request
     * @return a {@link ResponseEntity} with 405 Method Not Allowed status, indicating
     *         the unsupported method in the error message
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpRequestMethodNotSupportedException(
        HttpRequestMethodNotSupportedException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Method not supported: {} {} -> {}",
            request.getMethod(),
            request.getRequestURI(),
            ex.getMethod()
        );

        ProblemDetail problemDetail = errorResponse(
            HttpStatus.METHOD_NOT_ALLOWED,
            "Method not allowed",
            String.format(
                "Http method '%s' is not supported for this endpoint",
                ex.getMethod()
            ),
            null,
            request
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
            problemDetail
        );
    }

    /**
     * Handles unsupported media types in request content.
     *
     * This exception is thrown when the request's {@code Content-Type} header specifies
     * a media type that the endpoint cannot process. For example, sending XML (application/xml)
     * when the endpoint expects JSON (application/json).
     *
     * @param ex the exception containing the unsupported media type and supported
     *           media types for the endpoint
     * @param request the current HTTP request
     * @return a {@link ResponseEntity} with 415 Unsupported Media Type status, guiding
     *         the client to use application/json
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpMediaTypeNotSupportedException(
        HttpMediaTypeNotSupportedException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Media type not supported: {} {} -> {}",
            request.getMethod(),
            request.getRequestURI(),
            ex.getContentType()
        );

        ProblemDetail problemDetail = errorResponse(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Unsupported media type",
            "Content type in not supported. Use application/json",
            null,
            request
        );

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(
            problemDetail
        );
    }
}
