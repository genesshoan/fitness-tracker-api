# Frontend Integration

Keep the user profile and credential screens scoped to the current authenticated account.

| UI action | Request | Success |
|---|---|---|
| Load profile | `GET /api/v1/user/me` | Replace cached username/email |
| Change password | `PUT /api/v1/user/me/password` | Show success and keep the session unless authentication fails |
| Change username | `PUT /api/v1/user/me/username` | Refresh profile data |

Display field-level validation for blank values and passwords shorter than eight characters. For password changes, distinguish an incorrect old password (`401`) from a same-as-current or malformed request (`400`). Treat a username conflict (`409`) as a recoverable form error.

Do not display or persist fields that are not present in `UserResponseDTO`. The user's timezone affects statistics and should be treated as account configuration rather than a client-side date preference.
