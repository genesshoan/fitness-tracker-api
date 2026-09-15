# ADR-0003: Use Refresh Token Family IDs for Session-Level Revocation

- **Status:** Accepted
- **Date:** 2026-06-13

## Context

Refresh tokens are stored in the database so that they can be revoked when necessary.

However, storing refresh tokens alone is not enough to properly represent user sessions.

A user may be authenticated from multiple devices at the same time. For example, the same account may have an active session on a desktop computer and another active session on a mobile phone.

If a refresh token is compromised, revoking all refresh tokens belonging to the user would terminate every active session.

This is unnecessarily disruptive when only one session may have been compromised.

The authentication system therefore needs a way to group refresh tokens by session while still allowing individual sessions to be revoked independently.

## Decision

Associate refresh tokens with a **refresh token family ID**.

Each authenticated session receives its own family ID. Refresh tokens created as part of that session belong to the same family.

When a refresh token is rotated, the newly issued refresh token remains part of the same family.

This allows the application to distinguish between different sessions belonging to the same user.

For example:

- A user's desktop session has family ID `A`.
- The same user's mobile session has family ID `B`.
- Rotating the desktop refresh token keeps it within family `A`.
- Revoking family `A` invalidates the desktop session without affecting family `B`.

This provides session-level revocation without requiring all sessions belonging to a user to be revoked simultaneously.

## Consequences

### Positive

- Individual sessions can be revoked without terminating all sessions for a user.
- Multiple devices can remain independently authenticated.
- Refresh-token rotation preserves the identity of the session that issued the token.
- A compromised session can be isolated from the user's other active sessions.
- The database can represent the relationship between a user, their sessions, and their refresh-token rotations.

### Negative

- Refresh-token storage becomes more complex because tokens must be associated with a family.
- Session management requires additional database state and logic.
- Revocation logic must correctly distinguish between token-level and family-level operations.
- The authentication flow requires additional validation when rotating and revoking refresh tokens.

### Known Limitation

The current design does not provide individual revocation of already-issued access tokens.

Access tokens are stateless and are not stored in the database, so once an access token has been issued, the server cannot currently invalidate that specific token before it expires.

A possible future solution would be to introduce a timestamp or similar invalidation marker. For example, a user or session could have a `tokensValidAfter` timestamp, and access tokens issued before that timestamp would be rejected.

Another possible improvement would be to add explicit security notifications and session-management flows. For example, after detecting a suspicious login or receiving confirmation from the user that a login was not theirs, the application could revoke the affected session or all sessions and require authentication again.

These mechanisms are intentionally left for a future iteration.
