# Business Rules

## Authentication Model

The auth module implements a stateless JWT authentication model:
- **Access tokens** are short-lived (configured via `application.security.jwt.expiration`) and signed but not stored server-side. They contain the user's identity (subject) and roles.
- **Refresh tokens** are long-lived (configured via `application.security.jwt.refresh-token.expiration`) and persisted in the database for revocation and session control.

## Token Structure

### Access Token Claims
| Claim | Type | Description |
|-------|------|-------------|
| `sub` | String | User email (username) |
| `roles` | Array[String] | User roles/authorities |
| `iat` | Date | Issued at timestamp |
| `exp` | Date | Expiration timestamp |

### Refresh Token Claims
| Claim | Type | Description |
|-------|------|-------------|
| `sub` | String | User email (username) |
| `jti` | UUID | Unique token identifier (JWT ID) |
| `familyId` | UUID | Session identifier for token family grouping |
| `iat` | Date | Issued at timestamp |
| `exp` | Date | Expiration timestamp |

## Token Lifecycle

### Registration Flow (with Email Verification)
1. Validate uniqueness of username and email
2. Set user role to `USER`
3. Encode password using BCrypt
4. Persist user to database (email is initially unverified)
5. Generate verification token (SHA-256)
6. Send verification email with token
7. Do **not** issue access/refresh tokens until email is verified
8. User must call `POST /api/v1/auth/verify-email` before they can log in

### Login Flow
1. Authenticate via `AuthenticationManager`
2. Find user by email
3. Verify email is confirmed (else `EmailNotVerifiedException` → 403)
4. Generate new token family (UUID)
5. Create and persist refresh token record
6. Issue signed access and refresh tokens

### Email Verification Flow
1. User submits the token received by email to `POST /api/v1/auth/verify-email`
2. Token is hashed (SHA-256) and looked up
3. If the token is unknown, expired, already consumed, or belongs to another purpose → `InvalidCodeException` (400)
4. On success the account is marked as verified and the token is consumed
5. A new token family is created and access/refresh tokens are issued, so verification completes the login

### Resend Verification Flow
1. User submits the email to `POST /api/v1/auth/resend-verification`
2. If the account exists and is still unverified, a new verification email is sent
3. The response is identical in every case, so it never reveals whether an email is registered

### Password Reset Request Flow
1. User submits the email to `POST /api/v1/auth/forgot-password`
2. If the account exists, a single-use reset token is issued with the `PASSWORD_RESET` purpose, replacing any reset token previously issued for the account
3. A password reset email is sent with the token
4. The response is identical in every case, so it never reveals whether an email is registered

### Password Reset Flow
1. User submits the token received by email and the new password to `POST /api/v1/auth/reset-password`
2. Token is hashed (SHA-256) and looked up with the `PASSWORD_RESET` purpose
3. If the token is unknown, expired, already consumed, or belongs to another purpose → `InvalidCodeException` (400)
4. On success the token is consumed, the password hash is replaced, and all refresh tokens of the account are revoked
5. A password changed notification is sent to the account email

### Refresh Flow
1. Extract `jti` and `familyId` from refresh token
2. Look up refresh token in database by `jti`
3. If token already revoked → revoke entire family and reject (reuse detection)
4. Mark current token as revoked
5. Issue new access and refresh tokens with same `familyId`
6. Persist new refresh token record

### Logout Flow
1. Extract `familyId` from refresh token
2. Revoke all tokens with matching `familyId`
3. No database record deletion; tokens marked revoked

## Token Family Rotation

Refresh tokens are organized into a **token family** identified by `familyId`:
- A new `familyId` is generated at registration or login
- Token refresh preserves the original `familyId`
- Logout revokes the entire family at once

This enables:
- Single logout action to invalidate all refresh tokens of a session
- Logout from multiple devices
- Session-level token management

## Token Reuse Detection

If a revoked refresh token is used:
1. The entire token family is revoked
2. An `InvalidJwtException` is thrown
3. The user must re-authenticate

This is a security measure against token theft.

## User Constraints

### Registration
- Username must be unique
- Email must be unique
- Password must be at least 8 characters

### Login
- Email must exist
- Email must be verified (`emailVerified` = true; else `403` with `EmailNotVerifiedException`)
- Password must match the stored hash
- User must have a valid (non-revoked) refresh token to refresh

## Password Handling

- Passwords are encoded using BCrypt
- BCrypt provides adaptive hashing with built-in salt
- Password hashes are never returned in API responses
- Verification tokens (email verification and password reset) are purpose scoped, single use, and stored as SHA-256 hashes
- A password change or password reset revokes every refresh token of the account

## Default Role Assignment

All newly registered users are assigned the `USER` role:
```java
user.setRole(Role.USER);
```