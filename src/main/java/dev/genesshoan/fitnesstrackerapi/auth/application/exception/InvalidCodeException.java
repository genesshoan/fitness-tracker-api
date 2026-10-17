package dev.genesshoan.fitnesstrackerapi.auth.application.exception;

public class InvalidCodeException extends RuntimeException {

    public InvalidCodeException(String message) {
        super(message);
    }
}
