package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.genesshoan.fitnesstrackerapi.auth.application.exception.EmailNotVerifiedException;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.IncorrectPasswordException;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.InvalidCodeException;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.InvalidCredentialsException;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.InvalidRefreshTokenException;
import dev.genesshoan.fitnesstrackerapi.auth.application.exception.RefreshTokenReuseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static dev.genesshoan.fitnesstrackerapi.common.error.handler.ProblemDetailUtils.errorResponse;

@Slf4j
@Order(4)
@RestControllerAdvice
@RequiredArgsConstructor
public class AuthExceptionHandler {

    private final RefreshCookieFactory cookies;

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        logRejection("Invalid credentials", ex, request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorResponse(
                        HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid email or password", null, request));
    }

    @ExceptionHandler({InvalidRefreshTokenException.class, RefreshTokenReuseException.class})
    public ResponseEntity<ProblemDetail> handleSession(RuntimeException ex, HttpServletRequest request) {
        logRejection("Session rejected", ex, request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .body(errorResponse(
                        HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid or expired session", null, request));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ProblemDetail> handleEmailNotVerified(
            EmailNotVerifiedException ex, HttpServletRequest request) {
        logRejection("Email not verified", ex, request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorResponse(
                        HttpStatus.FORBIDDEN,
                        "Email not verified",
                        "Email address has not been verified",
                        null,
                        request));
    }

    @ExceptionHandler(InvalidCodeException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCode(InvalidCodeException ex, HttpServletRequest request) {
        logRejection("Invalid verification code", ex, request);
        return ResponseEntity.badRequest()
                .body(errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Invalid verification code",
                        "Invalid or expired verification code",
                        null,
                        request));
    }

    @ExceptionHandler(IncorrectPasswordException.class)
    public ResponseEntity<ProblemDetail> handleIncorrectPassword(
            IncorrectPasswordException ex, HttpServletRequest request) {
        logRejection("Incorrect current password", ex, request);
        return ResponseEntity.badRequest()
                .body(errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Current password is incorrect",
                        "Provided password is incorrect",
                        null,
                        request));
    }

    private void logRejection(String what, Exception ex, HttpServletRequest request) {
        log.warn("{}: {} {} -> {}", what, request.getMethod(), request.getRequestURI(), ex.getMessage());
    }
}
