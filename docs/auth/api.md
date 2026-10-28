# API Reference

## Endpoints

| Method | Path | Description | Rate limited |
|--------|------|-------------|--------------|
| POST | `/api/v1/auth/register` | Register a new user | IP + email |
| POST | `/api/v1/auth/verify-email` | Verify email address with token | No |
| POST | `/api/v1/auth/resend-verification` | Resend verification email | IP + email |
| POST | `/api/v1/auth/forgot-password` | Request a password reset email | IP + email |
| POST | `/api/v1/auth/reset-password` | Reset the password with a reset token | No |
| POST | `/api/v1/auth/login` | Authenticate user and get tokens | IP + email |
| POST | `/api/v1/auth/refresh` | Refresh access token using refresh token cookie | No |
| DELETE | `/api/v1/auth/logout` | Logout and revoke refresh token family | No |
| PATCH | `/api/v1/auth/password` | Change the authenticated user's password | No |

---

## Rate Limiting

The public credential endpoints are protected with Bucket4j. Before the
controller runs, every request is charged to two buckets:

- **IP bucket** — keyed by the client IP resolved from the proxy headers
  (`CF-Connecting-IP` behind Cloudflare). IPv4-mapped addresses (`::ffff:a.b.c.d`)
  are collapsed to IPv4, and IPv6 clients are grouped by `/64` so rotating the
  host part of the address does not reset the limit.
- **Account bucket** — keyed by the normalized email in the request body
  (trimmed and lower-cased). It limits attempts against one account even when the
  attacker rotates IP addresses.

A request is processed only when every configured bucket has capacity. If any
bucket is exhausted the API returns `429` and the request never reaches the
service layer. The quota is charged before the endpoint runs, so failed attempts
(wrong password, unknown email, duplicate registration) also count.

| Policy | Endpoint | IP limit | Account limit |
|--------|----------|----------|---------------|
| `LOGIN` | `POST /api/v1/auth/login` | 20 / minute | 5 / 15 minutes |
| `REGISTRATION` | `POST /api/v1/auth/register` | 5 / hour | 1 / minute and 3 / hour |
| `EMAIL_VERIFICATION` | `POST /api/v1/auth/resend-verification` | 10 / hour | 1 / 2 minutes and 5 / 12 hours |
| `PASSWORD_RESET` | `POST /api/v1/auth/forgot-password` | 10 / hour | 1 / minute and 3 / hour |

Limits without an explicit refill type (`LOGIN`) use continuous refill, so the
quota recovers token by token over the period. The others use `INTERVALLY`:
capacity is restored in full at the end of the period. When a policy declares
more than one band, all of them must have capacity.

### 429 Response

The response is a `ProblemDetail` (`Content-Type: application/problem+json`)
with a `Retry-After` header in seconds.

```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/problem+json
Retry-After: 60
```

```json
{
  "type": "about:blank",
  "title": "Too many requests",
  "status": 429,
  "detail": "Too many requests, retry later",
  "timestamp": "2026-01-01T12:00:00Z",
  "path": "/api/v1/auth/login"
}
```

Clients should wait for `Retry-After` seconds before retrying.

### Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `RATE_LIMIT_ENABLED` (`app.rate-limit.enabled`) | `true` | Enables or disables the rate limit aspect. The `test` profile sets it to `false` so integration tests do not share buckets between classes. |
| `app.rate-limit.ipv6-prefix` | `64` | Prefix used to group IPv6 clients. |

The limits live under `app.rate-limit.policies` in `application.yaml`. Only the
four endpoints above are rate limited; `verify-email`, `reset-password`,
`refresh`, `logout` and `password` change are not.

---

## Registration (`POST /api/v1/auth/register`)

**Description:** Creates a new user account with username, email, password, and timezone. A verification email is sent. No access token is returned until email is verified.

**Public endpoint** — does not require authentication.

**Rate limit:** `REGISTRATION` — 5 requests/hour per IP, 1/minute and 3/hour per email.

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
- `429` – Too many registration attempts for this IP or email (see [Rate Limiting](#rate-limiting))

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

**Rate limit:** `EMAIL_VERIFICATION` — 10 requests/hour per IP, 1/2 minutes and 5/12 hours per email.

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
- `429` – Too many verification email requests for this IP or email (see [Rate Limiting](#rate-limiting))

---

## Forgot Password (`POST /api/v1/auth/forgot-password`)

**Description:** Sends a password reset email if the account exists. The response is identical in every case so it does not reveal whether an email is registered. The reset token is single use and expires after `PASSWORD_RESET_EXPIRATION`.

**Public endpoint** — does not require authentication.

**Rate limit:** `PASSWORD_RESET` — 10 requests/hour per IP, 1/minute and 3/hour per email.

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
- `429` – Too many password reset requests for this IP or email (see [Rate Limiting](#rate-limiting))

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

**Rate limit:** `LOGIN` — 20 requests/minute per IP, 5/15 minutes per email.

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
- `429` – Too many login attempts for this IP or email (see [Rate Limiting](#rate-limiting))

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
