# ADR-0007: Use the AWS SDK for S3-Compatible Object Storage

- **Status:** Accepted
- **Date:** 2026-10-04

## Context

The application needs object storage for processed profile pictures. The
storage provider should be replaceable because the application may run with
different S3-compatible services depending on deployment needs and cost.

The original plan was to use MinIO's Java client. MinIO was later rejected
because the project no longer provides the maintenance level this application
needs for a foundational infrastructure dependency.

Using a provider-specific client would also couple the application to one
vendor and make future migrations more invasive.

## Decision

Use the AWS SDK for Java S3 directly through an application-defined storage
port.

The application exposes `ObjectStoragePort` from the common storage package.
The S3 infrastructure adapter implements that port and is configured with an
endpoint, region, credentials, bucket, path-style access, and presigned URL
expiration.

This supports AWS S3 and other S3-compatible services without introducing a
provider-specific client into the application or user modules.

The profile-picture service depends on `ObjectStoragePort`, not on the AWS SDK
or a concrete adapter. Provider-specific configuration and exceptions remain
inside the infrastructure boundary.

## Consequences

### Positive

- AWS S3 and other S3-compatible providers can be used through the same port.
- The application and user modules do not depend on the MinIO client.
- Storage provider changes are isolated to infrastructure configuration and
  adapter behavior.
- Presigned URLs can be generated without exposing storage credentials to
  clients.
- The AWS SDK is actively maintained as a broadly supported S3 client.

### Negative

- The application still depends on S3 semantics and is not storage-provider
  agnostic at the infrastructure level.
- S3-compatible providers can differ in signing, endpoint, and path-style
  behavior, so each provider requires configuration verification.
- The application owns the compatibility responsibility instead of delegating
  it to a provider-specific client.

### Security Considerations

Storage credentials remain server-side. Clients receive only temporary
presigned URLs, and storage keys are not included in API responses.

The adapter must wrap provider exceptions before they cross the infrastructure
boundary. HTTP error responses must not expose object keys, credentials,
provider messages, or local implementation details.

## Alternatives Considered

### MinIO Java Client

Rejected because MinIO's maintenance status made it unsuitable as a new
foundational dependency, and because it would couple the application to one
implementation even when the deployment target may be another S3-compatible
provider.

### Provider-Specific SDKs

Rejected because they would make changing providers require application-level
changes instead of an adapter/configuration change.
