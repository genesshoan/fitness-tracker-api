# ADR-0011: Simplify Session Management with Opaque Refresh Tokens

- **Status:** Accepted
- **Date:** 2026-10-22

## Context

The authentication module originally used JWTs for both access tokens and refresh tokens. However, refresh-token validation required a database lookup to check the token's persisted state, including revocation and expiration.

Because refresh tokens already depended on server-side state, using JWTs for them provided limited practical benefit over opaque tokens. An opaque token with sufficient cryptographic randomness can serve as a refresh credential without requiring the application to encode and validate JWT claims that are not needed by the refresh workflow.

The original access-token authentication flow also queried the database on every authenticated request to load the user. This reduced the benefits of stateless access-token validation and introduced additional database load.

The session redesign therefore aims to separate short-lived access authentication from stateful session renewal, reduce unnecessary database queries, and protect refresh credentials against common browser-based attacks.

## Decision

### 1. Use short-lived JWTs for access authentication

Continue using JWTs as access tokens, with a deliberately limited lifetime.

The JWT contains the information required to authenticate requests, including the user's identifier and authorities. The authentication filter validates the token and constructs the authentication context without loading the user from the database on every request.

The authentication context stores the user's identifier and authorities rather than a fully loaded user entity or user-details object.

Application services that require additional user data remain responsible for retrieving it through the appropriate application interfaces.

### 2. Use opaque refresh tokens

Replace JWT refresh tokens with cryptographically random opaque tokens.

Tokens are generated using a cryptographically secure random-number generator and contain 32 random bytes, encoded as an unpadded string.

The database stores a SHA-256 hash of each refresh token rather than its raw value. When a refresh request is received, the application hashes the presented token and looks up the corresponding record.

Refresh-token records retain the state needed to manage sessions, including the user identifier, token family, expiration, and revocation status.

Token rotation replaces a successfully used refresh token with a new token in the same family. Reuse of a revoked token triggers revocation of the associated family, subject to the defined transaction semantics.

### 3. Store refresh tokens in secure cookies

Return refresh tokens through a cookie configured with the `HttpOnly` and `Secure` attributes and an appropriate `SameSite` policy.

The browser must not expose the refresh token to ordinary client-side JavaScript. The cookie's domain, path, expiration, and other attributes must be configured to restrict its scope appropriately.

Access tokens remain separate from refresh-token storage and are sent through the Authorization header.

### 4. Apply CSRF protection to cookie-authenticated endpoints

Because browsers automatically attach eligible cookies to requests, cookie-based refresh credentials introduce a potential cross-site request forgery risk.

Enable CSRF protection for the refresh and logout endpoints that rely on the refresh-token cookie, rather than indiscriminately applying cookie-oriented CSRF handling to every bearer-token-authenticated API endpoint.

The CSRF mechanism must validate requests before allowing state-changing operations. Cookie scope and `SameSite` settings provide additional protection but are not treated as universal substitutes for CSRF validation.

## Consequences

### Positive

- Refresh tokens no longer require JWT claims or signature validation.
- Server-side refresh-token state supports rotation, expiration, revocation, and reuse detection.
- Raw refresh credentials are not stored in the database.
- Access-token authentication avoids a database lookup on every request by default.
- The authentication context contains only the information needed for request authentication.
- Refresh credentials are not directly accessible to ordinary client-side JavaScript.
- CSRF protection is focused on endpoints that rely on automatically attached refresh-token cookies.

### Negative

- Access-token authorities and identity claims may remain valid until token expiration, even if corresponding user state changes in the database.
- Refresh-token rotation and reuse detection require careful transaction handling to preserve session-family revocation semantics.
- Cookie-based authentication requires deliberate CSRF, CORS, and cookie configuration.
- Requests that need current user information must retrieve it separately.
- Opaque tokens require a database lookup during refresh, which is intentional because refresh-token state must be checked.

## Security Considerations

The refresh-token generator must use a cryptographically secure source of randomness. With 32 random bytes, the token provides 256 bits of entropy.

SHA-256 is appropriate for storing hashes of uniformly random, high-entropy refresh tokens. Unlike human passwords, these tokens are not vulnerable to practical dictionary attacks when generated correctly, so a deliberately slow password-hashing algorithm is not necessary for this purpose.

Refresh-token responses must not expose internal token hashes or session records. Refresh endpoints must validate token existence, expiration, revocation status, and rotation state.

Access-token expiration should limit the window during which outdated identity or authority claims remain usable. Sensitive operations may require additional checks against current server-side state.

Cookie configuration, CSRF validation, and CORS rules must be reviewed together, particularly if the frontend and API use different origins.

## Future Considerations

Evaluate access-token lifetime and revocation requirements as the application evolves.

If immediate access-token invalidation becomes necessary, consider an explicit revocation mechanism or server-side checks for operations that require current account state.

If the application introduces external identity providers, such as Google sign-in, reassess the authentication module's identity model and token lifecycle without unnecessarily coupling provider-specific behavior to the existing session-management mechanism.
