package dev.genesshoan.fitnesstrackerapi.auth.application.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
