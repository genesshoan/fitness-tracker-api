package dev.genesshoan.fitnesstrackerapi.common.error.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
