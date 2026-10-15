package dev.genesshoan.fitnesstrackerapi.auth.application.exception;

public class RefreshTokenReuseException extends RuntimeException {

    public RefreshTokenReuseException() {
        super("Revoked refresh token reuse detected");
    }
}
