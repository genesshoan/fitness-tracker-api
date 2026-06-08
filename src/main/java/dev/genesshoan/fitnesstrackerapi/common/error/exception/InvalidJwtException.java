package dev.genesshoan.fitnesstrackerapi.common.error.exception;

import io.jsonwebtoken.JwtException;

public class InvalidJwtException extends RuntimeException {
    public InvalidJwtException(String invalidOrExpiredToken, Exception ex) {
        super(invalidOrExpiredToken);
    }

    public InvalidJwtException(String invalidOrExpiredToken) {
        super(invalidOrExpiredToken);
    }
}
