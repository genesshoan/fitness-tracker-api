# API Reference

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Register a new user |
| POST | `/api/v1/auth/login` | Authenticate user and get tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token using refresh token |
| DELETE | `/api/v1/auth/logout` | Logout and revoke refresh token family |

## Registration (`POST /api/v1/auth/register`)

**Description:** Creates a new user account with username, email, and password.

**Request Body:**
```json
{
  "username": "string (required)",
  "password": "string (required, min 8 chars)",
  "email": "string (required)"
}
```

**Validation:**
- `username` – required, not blank
- `password` – required, not blank, minimum 8 characters
- `email` – required, valid email format

**Success Response (201):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Error Responses:**
- `400` – Invalid request data (missing fields, wrong format)
- `409` – Username or email already exists

## Login (`POST /api/v1/auth/login`)

**Description:** Authenticates a user with email and password, returning access and refresh tokens.

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
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Error Responses:**
- `401` – Invalid credentials (wrong email/password)
- `400` – Missing or malformed request

## Refresh (`POST /api/v1/auth/refresh`)

**Description:** Obtains a new access token and refresh token using a valid refresh token.

**Request Header:**
```http
Authorization: Bearer <refresh_token>
```

**Success Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Error Responses:**
- `400` – Missing or malformed Authorization header
- `401` – Invalid or expired refresh token

## Logout (`DELETE /api/v1/auth/logout`)

**Description:** Revokes all refresh tokens belonging to the same token family (session).

**Request Header:**
```http
Authorization: Bearer <refresh_token>
```

**Success Response (204):**
```
```

**Error Responses:**
- `401` – Invalid token

## Swagger/OpenAPI

The OpenAPI specification is defined in `OpenApiConfig.java` and specifies:
- Base path: `/api/v1/auth`
- Security scheme: `Bearer Authentication` (JWT)
- Public endpoints: registration and login
- Protected endpoints: all others require authentication

See [API Reference](api.md) for complete endpoint definitions.