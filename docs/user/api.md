# API Reference

All endpoints require a valid JWT access token and operate on the authenticated user.

| Method | Path | Request | Response |
|---|---|---|---|
| GET | `/api/v1/user/me` | none | `UserResponseDTO` |
| PUT | `/api/v1/user/me/profile-picture` | `multipart/form-data` with `file` | `ProfilePictureResponseDTO` |
| PUT | `/api/v1/user/me/password` | `ChangePasswordRequestDTO` | `204 No Content` |
| PUT | `/api/v1/user/me/username` | `ChangeUsernameRequestDTO` | `204 No Content` |

## Get Profile

`GET /api/v1/user/me`

Returns:

```json
{
  "email": "user@mail.com",
  "username": "user_123",
  "profilePictureUrl": "https://storage.example/profile-picture.jpg?signature=..."
}
```

`profilePictureUrl` is a temporary presigned URL and is `null` when no picture
has been uploaded. Clients should use the URL for display and request the
profile again when it expires. The response intentionally excludes the
password hash, role, timezone, storage key, and internal timestamps.

## Upload or Replace Profile Picture

`PUT /api/v1/user/me/profile-picture`

Send the image as the `file` part of a `multipart/form-data` request:

```bash
curl -X PUT \
  -H "Authorization: Bearer <access-token>" \
  -F "file=@avatar.png" \
  https://api.example.com/api/v1/user/me/profile-picture
```

The same operation handles the first upload and later replacements. The API
validates the image, processes it into the configured profile-picture format,
and returns:

```json
{
  "url": "https://storage.example/profile-picture.jpg?signature=..."
}
```

Invalid files return `400`. Storage and processing failures return `500` with a
generic detail; provider messages, local paths, and object-storage keys are not
exposed.

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
