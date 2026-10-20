package dev.genesshoan.fitnesstrackerapi.user.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.genesshoan.fitnesstrackerapi.user.application.exception.InvalidProfilePictureException;
import dev.genesshoan.fitnesstrackerapi.user.application.exception.ProfilePictureProcessingException;
import dev.genesshoan.fitnesstrackerapi.user.application.exception.UserRegistrationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static dev.genesshoan.fitnesstrackerapi.common.error.handler.ProblemDetailUtils.errorResponse;

@Slf4j
@Order(4)
@AllArgsConstructor
@RestControllerAdvice
public class UserExceptionHandler {

    /**
     * Handles user registration errors.
     *
     * @param ex      user registration exception from service layer
     * @param request current HTTP request
     * @return 500 Internal Server Error ProblemDetail response
     */
    @ExceptionHandler(UserRegistrationException.class)
    public ResponseEntity<ProblemDetail> handleUserRegistration(
            UserRegistrationException ex, HttpServletRequest request) {
        log.error("User registration failed: {} {}", request.getMethod(), request.getRequestURI(), ex);

        ProblemDetail problem = errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "User registration error",
                "The user could not be registered",
                null,
                request);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    /**
     * Handles invalid profile picture errors.
     *
     * @param ex      invalid profile picture exception from service layer
     * @param request current HTTP request
     * @return 400 Bad Request ProblemDetail response
     */
    @ExceptionHandler(InvalidProfilePictureException.class)
    public ResponseEntity<ProblemDetail> handleInvalidProfilePicture(
            InvalidProfilePictureException ex, HttpServletRequest request) {
        logRejection("Invalid profile picture", ex, request);

        ProblemDetail problem =
                errorResponse(HttpStatus.BAD_REQUEST, "Invalid profile picture", ex.getMessage(), null, request);

        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Handles profile picture processing errors.
     *
     * @param ex      profile picture processing exception from service layer
     * @param request current HTTP request
     * @return 500 Internal Server Error ProblemDetail response
     */
    @ExceptionHandler(ProfilePictureProcessingException.class)
    public ResponseEntity<ProblemDetail> handleProfilePictureProcessing(
            ProfilePictureProcessingException ex, HttpServletRequest request) {
        log.error("Profile picture processing failed: {} {}", request.getMethod(), request.getRequestURI(), ex);

        ProblemDetail problem = errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Image processing error",
                "The profile picture could not be processed",
                null,
                request);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private void logRejection(String what, Exception ex, HttpServletRequest request) {
        log.warn("{}: {} {} -> {}", what, request.getMethod(), request.getRequestURI(), ex.getMessage());
    }
}
