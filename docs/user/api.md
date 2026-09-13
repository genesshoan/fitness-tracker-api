# API Reference

All endpoints require a valid JWT access token and operate on the authenticated user.

| Method | Path | Request | Response |
|---|---|---|---|
| GET | `/api/v1/user/me` | none | `UserResponseDTO` |
| PUT | `/api/v1/user/me/password` | `ChangePasswordRequestDTO` | `204 No Content` |
| PUT | `/api/v1/user/me/username` | `ChangeUsernameRequestDTO` | `204 No Content` |

## Get Profile

`GET /api/v1/user/me`

Returns:

```json
{
  "email": "user@mail.com",
  "username": "user_123"
}
```

The response intentionally excludes the password hash, role, timezone, and internal timestamps.

## Change Password

`PUT /api/v1/user/me/password`

```json
{
  "oldPassword": "CurrentPass123!",
  "newPassword": "NewPass456!"
}
```

Both passwords are required and must contain at least eight characters. The old password is verified with the configured password encoder. The new password must differ from the current password.

## Change Username

`PUT /api/v1/user/me/username`

```json
{
  "newUsername": "newuser_144"
}
```

The username must not be blank, must differ from the current username, and must remain unique. Database uniqueness protects against duplicate usernames.

## Errors

| Status | Meaning |
|---|---|
| `400` | Validation failure or new credential matches the current value |
| `401` | Missing/invalid authentication or old password |
| `404` | Authenticated user no longer exists |
| `409` | Username uniqueness conflict |
