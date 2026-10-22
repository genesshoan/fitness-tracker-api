# API Reference

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Register a new user |
| POST | `/api/v1/auth/verify-email` | Verify email address with token |
| POST | `/api/v1/auth/resend-verification` | Resend verification email |
| POST | `/api/v1/auth/forgot-password` | Request a password reset email |
| POST | `/api/v1/auth/reset-password` | Reset the password with a reset token |
| POST | `/api/v1/auth/login` | Authenticate user and get tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token using refresh token cookie |
| DELETE | `/api/v1/auth/logout` | Logout and revoke refresh token family |
| PATCH | `/api/v1/auth/password` | Change the authenticated user's password |

---

## Registration (`POST /api/v1/auth/register`)

**Description:** Creates a new user account with username, email, password, and timezone. A verification email is sent. No access token is returned until email is verified.

**Public endpoint** — does not require authentication.

**Request Body:**
```json
{
  "username": "string (required, 3-30 chars)",
  "password": "string (required, min 8 chars, max 72)",
  "email": "string (required, valid email)",
  "timezone": "string (required, valid timezone)"
}
```

**Validation:**
- `username` – required, not blank, unique across system
- `password` – required, not blank, minimum 8 characters, maximum 72
- `email` – required, valid email format, unique across system
- `timezone` – required, valid IANA timezone identifier

**Success Response (201):**
```json
{
  "message": "Please check your email to verify your account"
}
```

**Error Responses:**
- `400` – Invalid request data (missing fields, wrong format, invalid timezone)
- `409` – Username or email already registered

---

## Verify Email (`POST /api/v1/auth/verify-email`)

**Description:** Verifies a user's email address using the single-use token sent by email. On success, the user is logged in (access and refresh tokens are issued).

**Public endpoint** — does not require authentication.

**Request Body:**
```json
{
  "token": "string (required)"
}
```

**Success Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

The refresh token is returned in an `HttpOnly`, `Secure` cookie named `refresh_token` with path `/api/v1/auth`.

**Error Responses:**
- `400` – Invalid request data (missing token, token unknown, expired, already used, or wrong purpose)

---

## Resend Verification Email (`POST /api/v1/auth/resend-verification`)

**Description:** Sends a new verification email if the account exists and is not yet verified. The response is identical in every case so it does not reveal whether an email is registered.

**Public endpoint** — does not require authentication.

**Request Body:**
```json
{
  "email": "string (required)"
}
```

**Success Response (204):**
```
```

**Error Responses:**
- `400` – Invalid request data (invalid email format)

---

## Forgot Password (`POST /api/v1/auth/forgot-password`)

**Description:** Sends a password reset email if the account exists. The response is identical in every case so it does not reveal whether an email is registered. The reset token is single use and expires after `PASSWORD_RESET_EXPIRATION`.

**Public endpoint** — does not require authentication.

**Request Body:**
```json
{
  "email": "string (required)"
}
```

**Success Response (204):**
```
```

**Error Responses:**
- `400` – Invalid request data (invalid email format)

---

## Reset Password (`POST /api/v1/auth/reset-password`)

**Description:** Resets the password using the single-use token sent by email. All refresh tokens of the user are revoked, so every session must log in again. A password changed notification is sent to the account email.

**Public endpoint** — does not require authentication.

**Request Body:**
```json
{
  "token": "string (required)",
  "newPassword": "string (required, min 8 chars)"
}
```

**Success Response (204):**
```
```

**Error Responses:**
- `400` – Missing or malformed request, token unknown, expired, already used, or wrong purpose, or new password equals current value

---

## Login (`POST /api/v1/auth/login`)

**Description:** Authenticates a user with email and password, returning an access token (body) and a refresh token (HttpOnly cookie). The user's email must be verified first.

**Public endpoint** — does not require authentication.

**Request Body:**
```json
{
  "email": "string (required)",
  "password": "string (required)"
}
```

**Success Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

The refresh token is returned in an `HttpOnly`, `Secure` cookie named `refresh_token` with path `/api/v1/auth`.

**Error Responses:**
- `401` – Invalid credentials (wrong email/password)
- `400` – Missing or malformed request
- `403` – Email address has not been verified (use `/api/v1/auth/verify-email` or `/api/v1/auth/resend-verification`)

---

## Refresh (`POST /api/v1/auth/refresh`)

**Description:** Rotates the refresh token cookie and issues a new access token. Reusing an already rotated (revoked) token revokes the entire token family.

**Public endpoint** — requires a valid refresh token cookie (not an Authorization header).

**Request Cookie:**
```
Cookie: refresh_token=<opaque_token>
```

**Request Header (CSRF):**
```
X-XSRF-TOKEN: <value of the XSRF-TOKEN cookie>
```

**Success Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

A new `refresh_token` cookie is set with the rotated token.

**Error Responses:**
- `400` – Refresh token cookie is missing or malformed
- `401` – Invalid or expired refresh token
- `403` – Missing or invalid CSRF token

---

## Logout (`DELETE /api/v1/auth/logout`)

**Description:** Revokes the refresh token family and clears the cookie. The access token remains valid until it expires.

**Public endpoint** — requires a valid refresh token cookie.

**Request Cookie:**
```
Cookie: refresh_token=<opaque_token>
```

**Request Header (CSRF):**
```
X-XSRF-TOKEN: <value of the XSRF-TOKEN cookie>
```

**Success Response (204):**
```
```

The `refresh_token` cookie is cleared by setting an empty, expired cookie.

**Error Responses:**
- `400` – Refresh token cookie is missing
- `403` – Missing or invalid CSRF token

---

## Change Password (`PATCH /api/v1/auth/password`)

**Description:** Changes the authenticated user's password after verifying the current one. All refresh tokens of the user are revoked, so every session must log in again once its access token expires.

**Protected endpoint** — requires a valid JWT access token (`Authorization: Bearer <access_token>`).

**Request Body:**
```json
{
  "oldPassword": "string (required)",
  "newPassword": "string (required, min 8 chars)"
}
```

**Success Response (204):**
```
```

**Error Responses:**
- `401` – Not authenticated, or current password is incorrect
- `400` – Missing or malformed request, or new password equals current value

---

## Swagger/OpenAPI

The OpenAPI specification is defined in `OpenApiConfig.java` and specifies:
- Base path: `/api/v1/auth`
- Security scheme: `Bearer Authentication` (JWT) for protected endpoints
- Cookie-based refresh token (`refresh_token` cookie) for refresh and logout
- Public endpoints: registration, email verification, resend verification, password reset request, password reset, login
- Protected endpoints: password change

See [API Reference](api.md) for complete endpoint definitions.
