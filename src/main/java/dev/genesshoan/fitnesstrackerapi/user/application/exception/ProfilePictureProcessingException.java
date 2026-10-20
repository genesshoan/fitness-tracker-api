package dev.genesshoan.fitnesstrackerapi.user.application.exception;

/**
 * Exception raised when image processing fails.
 *
 * This exception is thrown when an image cannot be processed,
 * such as during resizing, cropping, or format conversion.
 */
public class ProfilePictureProcessingException extends RuntimeException {

    public ProfilePictureProcessingException(String message) {
        super(message);
    }

    public ProfilePictureProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
