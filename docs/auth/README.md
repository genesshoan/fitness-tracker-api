# Authentication Module

JWT-based user authentication, refresh token rotation, and session management.

## Overview

The `auth` module provides stateless JWT authentication for the Fitness Tracker API. It manages the complete authentication lifecycle: user registration, login, access/refresh token issuance, refresh token rotation, and logout with token revocation.

Key responsibilities:

- Register new user accounts with username, email, and password
- Verify user email through single-use tokens sent by email
- Authenticate users via email and password
- Issue short-lived access tokens and long-lived opaque refresh tokens
- Rotate refresh tokens on each use to prevent reuse
- Revoke refresh token families on logout, password change or detected misuse
- Run the forgot-password and reset-password flows
- Enforce unique username and email constraints
- Throttle credential endpoints per IP and per account (see [Rate Limiting](business-rules.md#rate-limiting))

The module operates in a stateless manner for access tokens: they are not stored server-side and are validated via cryptographic signature. Refresh tokens are opaque random values whose SHA-256 hash is persisted, enabling server-side revocation, rotation, and session tracking without the token ever being readable from the database. The refresh token travels in an HTTP-only `refresh_token` cookie, never in the response body.

## Resources

- **User** — registered account with username, email, and encoded password
- **RefreshToken** — persistent refresh token record (stored as a hash) linked to a user and a token family
- **Token Family** — groups refresh tokens issued during the same login session
- **VerificationToken** — single-use token for email verification and password reset, stored as a hash

## Dependencies

- `user` module — provides `User`, `UserRepository`, `Role`, `UserMapper`
- `security` module — provides `JwtFilter`, `UserDetailsImpl`, `SecurityConfig`
- `common` module — provides exception types and error handling

## Architecture

![Auth Module Architecture](auth-architecture.png)

## Documentation

- [API Reference](api.md) — endpoint details, request/response schemas, error codes
- [Business Rules](business-rules.md) — token rotation, revocation, session management
- [Frontend Integration](frontend.md) — how to consume the auth API from a frontend