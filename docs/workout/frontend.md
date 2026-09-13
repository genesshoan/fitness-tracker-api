# Frontend Integration

Build the session editor around the `IN_PROGRESS` state. Once the finish endpoint succeeds, treat the session and all children as read-only.

The backend normalizes positions and set numbers. After adding, moving, or deleting an exercise, use the returned position list or reload the session instead of predicting sibling positions locally. Likewise, use the returned set number rather than the number submitted by the client.

For a new exercise or empty set form, the API may return values from the user's last completed set or category defaults. Display those values as suggestions, not as evidence that the user has completed the set.

Use `401` for authentication flow, `404` for resources outside the user's scope, and `400` for validation or completed-session mutation attempts. Show category-specific validation errors next to the affected exercise or set.
