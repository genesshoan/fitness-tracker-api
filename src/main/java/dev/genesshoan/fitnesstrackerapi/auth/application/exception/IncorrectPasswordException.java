package dev.genesshoan.fitnesstrackerapi.auth.application.exception;

public class IncorrectPasswordException extends RuntimeException {

    public IncorrectPasswordException(String message) {
        super(message);
    }
}
