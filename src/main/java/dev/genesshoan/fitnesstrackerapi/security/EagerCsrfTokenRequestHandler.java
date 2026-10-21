package dev.genesshoan.fitnesstrackerapi.security;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;

/**
 * Resolves the CSRF token on every request instead of leaving it deferred.
 *
 * <p>Spring Security only materializes the token when the application reads it, so a
 * stateless API that stores the token in a cookie would never write that cookie. This
 * handler resolves the token right away, so the {@code XSRF-TOKEN} cookie is always part
 * of the response and the client can echo it back in the {@code X-XSRF-TOKEN} header.
 *
 * <p>The plain attribute handler is used on purpose: the client reads the raw token from
 * the cookie and sends it back unchanged, which is what the cookie repository validates.
 */
public class EagerCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler delegate = new CsrfTokenRequestAttributeHandler();

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> deferredCsrfToken) {
        delegate.handle(request, response, deferredCsrfToken);

        // Resolving the token is what makes the cookie repository generate and store it.
        deferredCsrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        return delegate.resolveCsrfTokenValue(request, csrfToken);
    }
}
