# Business Rules

## Ownership and Identity

The authenticated principal supplies the user ID. The client cannot select another user for profile or credential operations.

## Password Changes

- Passwords are stored as encoded hashes, never as plaintext.
- The old password must match before a change is accepted.
- The new password must be different from the current password.
- Password changes do not automatically issue a new token pair; the current access token remains the caller's responsibility.

## Username Changes

- The new username must be non-blank.
- The new username must differ from the current username.
- Usernames are globally unique.
- A database uniqueness constraint remains the final protection against duplicates.

## Profile Exposure

The profile response is deliberately minimal: only email and username are exposed. Role, password hash, timezone, IDs, and persistence timestamps remain server-side.

## Timezone

The timezone is stored as an IANA timezone string with a maximum length of 50 characters. It is consumed by the stats module when converting workout timestamps into local active days and progress dates.
