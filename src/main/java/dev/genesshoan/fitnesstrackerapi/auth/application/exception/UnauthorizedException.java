package dev.genesshoan.fitnesstrackerapi.auth.application.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
