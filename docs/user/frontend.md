# Frontend Integration

Keep the user profile and credential screens scoped to the current authenticated account.

| UI action | Request | Success |
|---|---|---|
| Load profile | `GET /api/v1/user/me` | Replace cached username/email and display `profilePictureUrl` |
| Upload profile picture | `PUT /api/v1/user/me/profile-picture` with multipart `file` | Display returned `url` and refresh profile data |
| Change password | `PATCH /api/v1/auth/password` | Show success and keep the session unless authentication fails |
| Change username | `PUT /api/v1/user/me/username` | Refresh profile data |
| Verify email | `POST /api/v1/auth/verify-email` | Show success and re-authenticate if needed |
| Resend verification email | `POST /api/v1/auth/resend-verification` | Show success (does not reveal if email exists) |

Display field-level validation for blank values and passwords shorter than eight characters. For password changes, distinguish an incorrect old password (`401`) from a same-as-current or malformed request (`400`). Treat a username conflict (`409`) as a recoverable form error.

Profile-picture URLs are temporary. Do not persist the storage key or assume the URL is permanent; refresh the profile when the URL expires or when an image is replaced. Do not display or persist fields that are not present in `UserResponse`. The user's timezone affects statistics and should be treated as account configuration rather than a client-side date preference.

---

## Email Verification Flow

The user must verify their email address after registration before they can log in.

| UI action | Request | Success | Error |
|---|---|---|---|
| Verify email address | `POST /api/v1/auth/verify-email` with JSON body `{ "token": "string" }` | User is verified, can now log in | `400`: Invalid, expired, or already used token |
| Resend verification email | `POST /api/v1/auth/resend-verification` with JSON body `{ "email": "string" }` | New verification email sent | `400`: Invalid request data |

### Verification Sequence

1. During registration (`POST /api/v1/auth/register`), the backend sends a verification email to the provided address
2. The email contains a one-time token link or the token itself
3. User clicks the link or pastes the token into the verification form
4. Submit `POST /api/v1/auth/verify-email` with the token
5. Backend validates the token and marks the user's email as verified
6. User can now log in with their credentials

If the verification email expires or is lost, use **Resend verification email** to send a new one.

---

## Error Handling

| Status | Meaning | Frontend Action |
|---|---|---|
| `400` | Validation failure or new credential matches current value / invalid token | Show field errors or global error message |
| `401` | Missing/invalid authentication or old password | Show "Invalid credentials" or redirect to login |
| `404` | Authenticated user no longer exists | Show "User not found, please log in again" and clear session |
| `409` | Username uniqueness conflict | Show "Username already taken, please choose another" |
| `500` | Profile picture processing or storage failed | Show generic error, retry or use existing picture |