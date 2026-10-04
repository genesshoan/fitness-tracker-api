# ADR-0008: Process Profile Picture Uploads in the Backend

- **Status:** Accepted
- **Date:** 2026-10-04

## Context

Profile pictures must be validated, decoded, resized, cropped, converted to a
known format, and stored under a user-owned key. The backend already owns the
user identity and the profile-picture rules, so it can perform these steps as
one operation.

An alternative design would have the client upload directly to object storage
using a presigned upload policy. That would require the backend to coordinate
post-upload processing and persistence. Depending on the storage flow, this
could require message queues, storage webhooks, polling, retry orchestration,
or a separate asynchronous processing worker.

That architecture can be appropriate for large files or high-volume media,
but it introduces operational complexity that is not justified for small
profile pictures.

## Decision

Keep profile-picture uploads behind the backend.

The authenticated client sends the image as multipart form data to
`PUT /api/v1/user/me/profile-picture`. The backend:

1. Resolves the user from the authenticated principal.
2. Validates the file size, detected type, and image dimensions.
3. Processes the image into the configured profile-picture format.
4. Uploads the processed image through `ObjectStoragePort`.
5. Persists the stable object key on the user.
6. Returns a temporary presigned URL.

The same operation handles both the first upload and replacement. Reusing the
user-specific key lets object storage replace the previous object without a
separate delete workflow.

## Consequences

### Positive

- Validation and image processing complete before the request succeeds.
- The user key is persisted atomically with the application operation.
- The backend can return a consistent response containing the access URL.
- No queue, webhook endpoint, polling loop, or processing worker is required.
- The client does not need to coordinate storage completion with the API.
- The implementation is appropriate for the expected size and frequency of
  profile-picture uploads.

### Negative

- The backend receives and temporarily buffers the upload.
- Request duration includes validation, processing, and object-storage latency.
- Backend bandwidth and memory usage grow with upload size.
- Large media uploads or high-volume media workflows may require a separate
  asynchronous architecture later.

### Security Considerations

The authenticated principal determines ownership; clients cannot submit an
arbitrary user ID. The backend validates the detected file content rather than
trusting only the filename or client-provided MIME type.

Storage keys are not returned to clients. Processing, storage, and provider
errors are translated into generic API details so internal paths, keys, and
provider messages are not disclosed.

## Future Considerations

If profile pictures become significantly larger or upload volume increases,
the system can introduce direct-to-storage uploads and asynchronous processing.
That change should also introduce an explicit state model, retry policy,
idempotency strategy, and webhook or queue contract rather than bypassing the
current validation rules.
