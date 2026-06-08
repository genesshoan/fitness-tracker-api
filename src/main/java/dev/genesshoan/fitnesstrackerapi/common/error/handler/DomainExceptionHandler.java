package dev.genesshoan.fitnesstrackerapi.common.error.handler;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.*;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static dev.genesshoan.fitnesstrackerapi.common.error.handler.ProblemDetailUtils.errorResponse;

/**
 * Global exception handler for domain/business logic exceptions.
 *
 * <p>
 * This handler is responsible for translating business-level exceptions
 * thrown from services (e.g. AuthService) into standardized HTTP responses
 * using RFC 7807 ProblemDetail format.
 *
 * <p>
 * It covers:
 * <ul>
 * <li>Resource not found scenarios (404)</li>
 * <li>Conflict scenarios such as duplicate users (409)</li>
 * <li>Bad request business logic errors (400)</li>
 * <li>JWT authentication/authorization errors (401)</li>
 * </ul>
 *
 * <p>
 * This layer ensures services remain free of HTTP concerns and only
 * focus on business rules.
 */
@Slf4j
@Order(4)
@RestControllerAdvice
public class DomainExceptionHandler {

  /**
   * Handles cases where a requested resource does not exist.
   *
   * Typical cases:
   * - User not found
   * - Entity lookup failures
   *
   * @param ex      exception thrown by service layer
   * @param request current HTTP request
   * @return 404 ProblemDetail response
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(
      ResourceNotFoundException ex,
      HttpServletRequest request) {

    log.warn("Resource not found: {} {} -> {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getMessage());

    ProblemDetail problem = errorResponse(
        HttpStatus.NOT_FOUND,
        "Resource not found",
        ex.getMessage(),
        null,
        request);

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  /**
   * Handles conflict scenarios where a resource already exists.
   *
   * Typical cases:
   * - Duplicate username
   * - Duplicate email
   *
   * @param ex      exception thrown by service layer
   * @param request current HTTP request
   * @return 409 Conflict ProblemDetail response
   */
  @ExceptionHandler(ResourceAlreadyExistsException.class)
  public ResponseEntity<ProblemDetail> handleConflict(
      ResourceAlreadyExistsException ex,
      HttpServletRequest request) {

    log.warn("Conflict: {} {} -> {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getMessage());

    ProblemDetail problem = errorResponse(
        HttpStatus.CONFLICT,
        "Resource already exists",
        ex.getMessage(),
        null,
        request);

    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  /**
   * Handles invalid JWT token scenarios.
   *
   * Typical cases:
   * - Expired token
   * - Malformed token
   * - Invalid signature
   * - Token reuse detection
   *
   * @param ex      JWT exception thrown during validation
   * @param request current HTTP request
   * @return 401 Unauthorized ProblemDetail response
   */
  @ExceptionHandler(InvalidJwtException.class)
  public ResponseEntity<ProblemDetail> handleInvalidJwtException(
          InvalidJwtException ex,
          HttpServletRequest request) {

    log.warn("JWT error: {} {} -> {}",
            request.getMethod(),
            request.getRequestURI(),
            ex.getMessage());

    ProblemDetail problem = errorResponse(
            HttpStatus.UNAUTHORIZED,
            "Invalid or expired token",
            ex.getMessage(),
            null,
            request);

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
  }

  /**
   * Handles business-level bad request errors.
   *
   * Typical cases:
   * - Missing or malformed Authorization header
   * - Invalid business state
   *
   * @param ex      bad request exception from service layer
   * @param request current HTTP request
   * @return 400 Bad Request ProblemDetail response
   */
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ProblemDetail> handleBadRequest(
      BadRequestException ex,
      HttpServletRequest request) {

    log.warn("Bad request: {} {} -> {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getMessage());

    ProblemDetail problem = errorResponse(
        HttpStatus.BAD_REQUEST,
        "Bad request",
        ex.getMessage(),
        null,
        request);

    return ResponseEntity.badRequest().body(problem);
  }

  /**
   * Handles business-level nauthorized request errors.
   *
   * Typical cases:
   * - Wrong username or password
   *
   * @param ex      invalid credentials exception from service layer
   * @param request current HTTP request
   * @return 401 Unauthorized ProblemDetail response
   */
  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ProblemDetail> handleInvalidCredentials(
      InvalidCredentialsException ex,
      HttpServletRequest request) {

    ProblemDetail problem = errorResponse(
        HttpStatus.UNAUTHORIZED,
        "Unauthorized",
        "Invalid email or password",
        null,
        request);

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(problem);
  }
}
