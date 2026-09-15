# ADR-0002: Use Stateless Access Tokens and Stateful Refresh Tokens

- **Status:** Accepted
- **Date:** 2026-06-10

## Context

The application needs an authentication mechanism that supports authenticated requests while also allowing users to remain logged in for an extended period of time.

JWTs were chosen partly because implementing token-based authentication was an important learning goal for this project. The application is currently a monolith, but JWT-based authentication can also be useful in distributed architectures and microservices because access tokens can be validated without requiring a centralized session store for every request.

However, using the same strategy for both short-lived access tokens and long-lived refresh tokens introduces security concerns.

Access tokens are intentionally short-lived, which limits the impact of a compromised token. Refresh tokens have a significantly longer lifetime and therefore represent a greater security risk if compromised.

The application therefore needs different handling for the two types of tokens.

## Decision

Use a **stateless access token** combined with a **stateful refresh token**.

### Access Tokens

Access tokens are JWTs and are not persisted in the database.

They are short-lived and contain the information required by the application to authenticate and authorize requests.

The server validates the token's signature and claims without looking up the token in a database on every request.

This keeps normal authenticated requests stateless and avoids introducing a database dependency into every authentication check.

### Refresh Tokens

Refresh tokens are stored in the database and are associated with the corresponding user session.

Unlike access tokens, refresh tokens are treated as stateful credentials. This allows them to be explicitly revoked when necessary, for example when a session is terminated or a token is suspected to have been compromised.

Refresh token rotation is used so that a refresh token is not continuously reusable throughout its entire lifetime.

## Consequences

### Positive

- Normal authenticated requests remain stateless.
- Access-token validation does not require a database lookup on every request.
- Short-lived access tokens limit the impact of token theft.
- Refresh tokens can be explicitly revoked.
- Individual user sessions can be tracked independently.
- The architecture can be extended to a distributed or microservice environment without requiring a centralized session store for every access-token validation.

### Negative

- Refresh tokens require persistent storage.
- The authentication system is more complex than using a single stateless token.
- Token rotation and revocation require additional database operations and logic.
- A compromised access token cannot be individually revoked through the current design before it expires.

### Security Considerations

The different treatment of access and refresh tokens is intentional.

Access tokens have a shorter lifetime and are therefore treated as disposable credentials. Refresh tokens have a longer lifetime and are persisted specifically so that they can be revoked.

Refresh-token compromise is further addressed through token rotation and refresh-token family tracking, which is documented separately in ADR-0003.
