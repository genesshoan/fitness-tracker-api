package dev.genesshoan.fitnesstrackerapi.common.error.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
