package dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.genesshoan.fitnesstrackerapi.ratelimit.application.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;

import static dev.genesshoan.fitnesstrackerapi.common.error.handler.ProblemDetailUtils.errorResponse;

@Slf4j
@Order(5)
@RestControllerAdvice
public class RateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(
            RateLimitExceededException ex, HttpServletRequest request) {
        logRejection(ex, request);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.getRetryAfterSeconds()))
                .body(errorResponse(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too many requests",
                        "Too many requests, retry later",
                        null,
                        request));
    }

    private void logRejection(RateLimitExceededException ex, HttpServletRequest request) {
        log.warn(
                "Rate limit exceeded: {} {} -> retry after {}s",
                request.getMethod(),
                request.getRequestURI(),
                ex.getRetryAfterSeconds());
    }
}
