# Business Rules

## Record Creation

### Uniqueness Constraint

Each user can have at most one progress record per date. This is enforced by:
- Database unique constraint on `(user_id, recorded_at)`
- Service layer catches `DataIntegrityViolationException` on save and converts to `ResourceAlreadyExistsException`

This prevents users from having multiple measurements on the same day.

### Immutable Fields

Once created, these fields cannot be changed:
- `recordedAt` - the measurement date
- `weightKg` - the weight value

To correct a record, delete and recreate it.

### Body Fat Percentage

`bodyFatPercentage` is optional:
- If provided, must be between 0 and 100 (inclusive)
- If not provided, the field is null in the response

## Date Range Query

### Validation

- `from` must not be after `to` — throws `BadRequestException`
- Both dates are inclusive in the query results
- Records are returned in ascending date order (oldest first)

### Pagination

- Uses standard Spring Data pagination
- Results are sorted by `recordedAt` ascending
- Cursor pagination is not used; standard offset-based pagination

## Ownership

All operations are user-scoped:
- The authenticated user's ID is extracted from `UserDetailsImpl` (via `@AuthenticationPrincipal`)
- Records can only be created, read, and deleted by their owner
- Delete operations check `existsByIdAndUserId` before deletion — returns 404 if the record doesn't belong to the user

## Data Model

| Field | Type | Nullable | Mutable | Description |
|-------|------|----------|---------|-------------|
| id | UUID | No | No | Auto-generated identifier |
| recordedAt | LocalDate | No | No | Date of measurement |
| weightKg | Double | No | No | Weight in kilograms |
| bodyFatPercentage | Double | Yes | Yes | Body fat percentage (0-100) |
| user | User | No | No | Record owner (lazy-loaded) |

### Database Constraints

- Unique constraint on `(user_id, recorded_at)`
- `recordedAt` and `weightKg` columns are NOT NULL
- `user_id` is a foreign key to `users` table
