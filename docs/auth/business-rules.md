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

### Registration Flow
1. Validate uniqueness of username and email
2. Set user role to `USER`
3. Encode password using BCrypt
4. Persist user to database
5. Generate new token family (UUID)
6. Create and persist refresh token record
7. Issue signed access and refresh tokens

### Login Flow
1. Authenticate via `AuthenticationManager`
2. Find user by email
3. Generate new token family (UUID)
4. Create and persist refresh token record
5. Issue signed access and refresh tokens

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
- Password must match the stored hash
- User must have a valid (non-revoked) refresh token to refresh

## Password Handling

- Passwords are encoded using BCrypt
- BCrypt provides adaptive hashing with built-in salt
- Password hashes are never returned in API responses

## Default Role Assignment

All newly registered users are assigned the `USER` role:
```java
user.setRole(Role.USER);
```