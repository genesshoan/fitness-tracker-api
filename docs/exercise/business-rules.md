# Business Rules

## Exercise Catalog

### Soft Deletion

Exercises use a soft delete model via the `active` flag:
- `active = true` — exercise is visible and returned by API
- `active = false` — exercise is hidden from all read operations
- No DELETE endpoint exists; exercises are never hard-deleted

This means:
- `GET /api/v1/exercises` only returns active exercises
- `GET /api/v1/exercises/{slug}` only returns active exercises
- `findBySlugAndActiveTrue` and `findByFiltersAndActiveTrue` enforce the active filter

### Filtering Logic

The `GET /api/v1/exercises` endpoint supports multi-field filtering:
- All filters are combined with AND logic
- Null/absent filters are ignored (no filtering applied)
- Filters are processed at the repository level via JPQL

Supported filters:
- **category** — must match exactly (STRENGTH, CARDIO, MOBILITY)
- **difficulty** — must match exactly (BEGINNER, INTERMEDIATE, ADVANCED)
- **muscleSlugs** — exercise must be associated with any of the specified muscles

### Cursor Pagination

Pagination uses cursor-based pagination:
- `cursor` — the last ID from the previous page; null for first page
- `size` — number of items per page (max 100); default applied by pageable
- Results are ordered by ID ascending
- Cursor ensures consistent ordering across pages

## Exercise-Muscle Relationship

### Impact Levels

Each exercise-muscle relationship has an impact level:
- **PRIMARY** — the main target muscle for the exercise
- **SECONDARY** — a supporting muscle that also works significantly
- **STABILIZER** — a muscle that helps stabilize the movement

### Data Loading

- `GET /api/v1/exercises/{slug}` loads exercise muscles eagerly via `@EntityGraph`
- `GET /api/v1/exercises` loads only exercise list items (no muscles)
- Exercise muscles are lazy-loaded in detail views; must be initialized within an active persistence context

## Muscle Catalog

- Muscles are organized by `BodyRegion` (CHEST, BACK, SHOULDERS, ARMS, CORE, LEGS, OTHER)
- Each muscle has a unique slug and name
- No write operations are exposed via API

## Category Validation

Each exercise category defines validation rules for exercise metrics:

| Category | Validation Rules | Default Metrics |
|----------|------------------|-----------------|
| STRENGTH | reps > 0, weightKg > 0, no distance or duration | reps=8, weightKg=5 |
| CARDIO | durationSeconds > 0 or distanceKm > 0, no reps or weight | durationSeconds=30 |
| MOBILITY | durationSeconds > 0, no reps, weight, or distance | durationSeconds=30 |