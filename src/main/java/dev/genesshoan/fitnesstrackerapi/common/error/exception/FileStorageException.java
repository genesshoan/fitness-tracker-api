package dev.genesshoan.fitnesstrackerapi.common.error.exception;

/**
 * Exception raised when file storage operations fail.
 *
 * This exception is thrown when there are issues with uploading,
 * downloading, or managing files in the storage system.
 */
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
