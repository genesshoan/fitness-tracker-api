package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.web;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.config.RefreshTokenProperties;
import lombok.RequiredArgsConstructor;

/**
 * Builds the HttpOnly cookie that carries the refresh token.
 *
 * <p>Creation and clearing share the same name, path and attributes. This matters
 * because a browser only removes a cookie when the clearing {@code Set-Cookie}
 * matches the one that created it.
 */
@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

    /** Cookie name. Constant so it can be used in {@code @CookieValue}. */
    public static final String COOKIE_NAME = "refresh_token";

    private static final String COOKIE_PATH = "/api/v1/auth";
    private static final String SAME_SITE = "Lax";

    private final RefreshTokenProperties properties;

    /**
     * Creates the cookie that stores a refresh token.
     *
     * @param refreshToken the raw refresh token
     * @return the cookie, valid for the configured refresh token lifetime
     */
    public ResponseCookie create(String refreshToken) {
        return base(refreshToken).maxAge(properties.expiration()).build();
    }

    /**
     * Creates a cookie that makes the browser delete the refresh token cookie.
     *
     * @return an expired, empty cookie with the same attributes as the original
     */
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .path(COOKIE_PATH)
                .sameSite(SAME_SITE);
    }
}
