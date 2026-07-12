package dev.genesshoan.fitnesstrackerapi.common.error.exception;

import java.util.List;
import java.util.Map;

public class ValidationException extends RuntimeException {

    private final Map<String, List<String>> errors;

    public ValidationException(Map<String, List<String>> errors) {
        super("One or more fields are invalid.");
        this.errors = Map.copyOf(errors);
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }
}
