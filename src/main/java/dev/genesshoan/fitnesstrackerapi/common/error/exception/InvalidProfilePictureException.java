package dev.genesshoan.fitnesstrackerapi.common.error.exception;

/**
 * Exception raised when a profile picture fails validation.
 * 
 * This exception is thrown when the uploaded image does not meet
 * the required criteria, such as dimensions, file type, or size limits.
 */
public class InvalidProfilePictureException extends RuntimeException {

    public InvalidProfilePictureException(String message) {
        super(message);
    }

    public InvalidProfilePictureException(String message, Throwable cause) {
        super(message, cause);
    }
}
