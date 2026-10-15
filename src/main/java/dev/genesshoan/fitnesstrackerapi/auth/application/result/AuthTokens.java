package dev.genesshoan.fitnesstrackerapi.auth.application.result;

/**
 * Result record representing a pair of authentication tokens.
 *
 * <p>Contains both the short-lived JWT access token and the opaque refresh token
 * used for token rotation. This record is returned by login, refresh, and
 * other authentication operations.
 *
 * @param accessToken  the JWT access token for API authorization
 * @param refreshToken the opaque refresh token for session maintenance
 * @see AuthTokens
 */
public record AuthTokens(String accessToken, String refreshToken) {}
