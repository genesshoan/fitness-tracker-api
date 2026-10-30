# Business Rules

## Exercise Catalog

### Soft Deletion

Exercises use a soft delete model via the `active` flag:
- `active = true` — exercise is visible and returned by API
- `active = false` — exercise is hidden from all read operations
- The `DELETE /api/v1/exercises/{slug}` endpoint (ADMIN only) only flips the flag; exercises are never hard-deleted

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

### Write Operations (ADMIN only)

`POST`, `PUT` and `DELETE /api/v1/exercises/**` require the `ADMIN` role (`hasRole("ADMIN")` in `SecurityConfig`); other authenticated users receive `403`.

- **Create** — the slug must be unique across active and inactive exercises (`409` on conflict); every `muscles[].muscleSlug` must reference an existing muscle (`404` otherwise); duplicated muscle slugs are rejected with `400`; null `instructions` default to an empty list. New exercises are created active.
- **Full update** — the supplied `muscles` list replaces all current associations (orphans are removed). The slug may be changed as long as the new value is free (`409` on conflict).
- **Delete** — soft delete only (`active = false`); the slug stays reserved, so recreating an exercise with the same slug returns `409` unless the slug is changed.

## Exercise-Muscle Relationship

### Impact Levels

Each exercise-muscle relationship has an impact level:
- **PRIMARY** — the main target muscle for the exercise
- **SECONDARY** — a supporting muscle that also works significantly
- **STABILIZER** — a muscle that helps stabilize the movement

Seed data populates all three levels (stabilizers were backfilled for free compound, unilateral, overhead, suspension and instability exercises).

### Instructions and Media

- `instructions` — ordered step-by-step execution guide, persisted as a Postgres `TEXT[]` array column and exposed in the detail DTO as a JSON string array (nullable).
- `media_object_key` — nullable internal object-storage key (e.g. `exercises/abc123.gif`). Infrastructure detail: stored on the entity, **never exposed** in any API response, and assigned by the media pipeline.
- `thumbnail_object_key` — nullable internal object-storage key of the exercise thumbnail, with the same guarantees as `media_object_key`.
- `gifUrl` — public URL of the demonstration GIF, resolved from `media_object_key` against the configured catalog base URL. `null` when the exercise has no media assigned.
- `thumbnailUrl` — public URL of the exercise thumbnail, resolved from `thumbnail_object_key`. `null` when the exercise has no thumbnail assigned.

Both URLs are resolved in the catalog module and exposed only as absolute URLs; the storage keys never reach the client.

### Seed Data Review (feature/muscle-enhancement)

The `seeds/exercises.yml` catalog was biomechanically reviewed:
- 63 static-stretch / mobility drills reclassified `STRENGTH → MOBILITY`
- `battling-ropes`, `quick-feet-v-2`, `wind-sprints` reclassified `STRENGTH → CARDIO`
- `wheel-run` (ab-wheel rollout) reclassified `CARDIO → STRENGTH`
- Stabilizer relationships populated for 190 exercises
- 2 exact duplicate slugs disambiguated with a `-v2` suffix
- `difficulty` was left untouched (all `INTERMEDIATE` in source data)
- `V6__seed_exercise_muscle_data.sql` is regenerated from the YAML via `./gradlew seed` (note: each run generates new UUIDs)

### Exercise Names

The display name of an exercise has its first letter in capital and the rest in lower case (`Barbell bench press`). The catalog source data is fully lower case, so `V16__capitalize_exercise_names.sql` normalizes the seeded rows after `V6` inserts them.

- Only the first letter is capitalized; the rest of the name keeps its original casing.
- The first letter is the first alphabetic character, so names that start with a digit keep their leading part (`3/4 Sit-up`).
- The `slug` is not modified by the migration: it is the stable public identifier of the exercise and is already lower case.
- Search and the name highlighting are case-insensitive, so the normalization does not change how exercises are found or rendered.

### Exercise Name Encoding

The degree sign of the "45°" exercises was mis-encoded when the catalog was parsed: the UTF-8 bytes were decoded as Latin-1 and stored again as UTF-8, so `°` came out as `Â°`, and in a few names as `Ð²Â°`. `V17__fix_exercise_name_encoding.sql` replaces both corrupted sequences by the degree sign again.

- Both the `name` and the `description` are repaired; the `slug` never contained the degree sign and is not touched.
- The description keeps its original casing: only the encoding is fixed, the capitalization rule applies to the name only.
- The migration is written with the `E'\uXXXX'` escape syntax so that the file itself only contains ASCII and cannot be mis-encoded again.
- `V6__seed_exercise_muscle_data.sql` is left untouched on purpose: changing an already applied migration breaks the Flyway checksum validation.

### Data Loading

- `GET /api/v1/exercises/{slug}` loads exercise muscles eagerly via `@EntityGraph`
- `GET /api/v1/exercises` loads only exercise list items (no muscles)
- Exercise muscles are lazy-loaded in detail views; must be initialized within an active persistence context

## Muscle Catalog

- Muscles are organized by `BodyRegion` (CHEST, BACK, SHOULDERS, ARMS, CORE, LEGS, OTHER)
- Each muscle has a unique slug and name
- The complete slug catalog is documented in [Muscle slugs](README.md#muscle-slugs); clients must use those slugs in filters and write requests.
- No write operations are exposed via API

## Category Validation

Each exercise category defines validation rules for exercise metrics:

| Category | Validation Rules | Default Metrics |
|----------|------------------|-----------------|
| STRENGTH | reps > 0, weightKg > 0, no distance or duration | reps=8, weightKg=5 |
| CARDIO | durationSeconds > 0 or distanceKm > 0, no reps or weight | durationSeconds=30 |
| MOBILITY | durationSeconds > 0, no reps, weight, or distance | durationSeconds=30 |


