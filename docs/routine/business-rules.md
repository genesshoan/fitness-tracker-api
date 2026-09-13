# Business Rules

## Routine Ownership

Each routine belongs to exactly one user. The authenticated user's ID is extracted from the JWT token and validated against the routine's owner. Users can only access and modify their own routines.

All operations validate ownership via `findAndValidateRoutine`:
- `findByIdAndActiveTrue` enforces the active filter
- `findByIdAndUserIdAndActiveTrue` ensures the user owns the routine

## Uniqueness Constraint

Routine names must be unique per user. The system enforces this constraint through the `existsByNameAndUserIdAndActiveTrue` method. If a user attempts to create or update a routine with a name that already exists, a `ResourceAlreadyExistsException` is thrown.

This applies to both creation and update operations.

## Soft Deletion

Routines use a soft delete model:
- `active = true` — routine is visible and accessible
- `active = false` — routine is hidden from all operations
- No hard delete operations are exposed via API

The `findByIdAndActiveTrue` and `findAllByUserIdAndActiveTrueWithExerciseCount` methods enforce the active filter.

## Exercise Positioning

Exercises within a routine maintain a specific order based on their position property. The position is a 1-based integer that represents the order of exercises in the routine. The system ensures that positions are unique within a routine through a database unique constraint (`uk_routine_exercise_position` on `routine_id` and `position`).

When adding an exercise at a specific position:
1. The position is validated and adjusted if necessary (clamped to valid range)
2. Existing exercises at or after that position are shifted down by 1
3. The new exercise is inserted at the specified position

When removing an exercise:
1. The exercise is removed from the list
2. Remaining exercises after the removed position are shifted up by 1

When updating an exercise:
1. The existing exercise at the position is removed
2. A new exercise is built and added at the same position

## Validation Rules

### Routine Creation
- Name is required and cannot be blank
- Description is optional
- Exercises are optional but must be valid exercise IDs
- Each exercise must exist and be active
- All exercise IDs must be resolvable (no missing exercises)

### Exercise Metrics Validation
Each exercise category has specific validation rules enforced by `ExerciseCategory.validate()`:
- **STRENGTH** – requires reps and weightKg
- **CARDIO** – requires durationSeconds or distanceKm
- **MOBILITY** – requires durationSeconds

Validation is performed per-exercise in `resolveAndValidateExercises`. If any exercise fails validation, a `ValidationException` is thrown with a map of exercise IDs to error messages.

### Position Validation
- Position must be >= 1
- Position is clamped to valid range [1, maxPosition + 1] for add operations
- For update operations, position must match an existing exercise

## Data Model

| Field | Type | Nullable | Mutable | Description |
|-------|------|----------|---------|-------------|
| id | UUID | No | No | Auto-generated identifier |
| name | String | No | Yes | Name of the routine |
| description | String | Yes | Yes | Description of the routine |
| active | Boolean | No | Yes | Soft delete flag |
| user | User | No | No | Record owner (lazy-loaded) |
| exercises | List<RoutineExercise> | No | Yes | Ordered list of exercises |

### Database Constraints

- Unique constraint on `(name, user_id)` for routine names
- Unique constraint on `(routine_id, position)` for exercise positions
- `name` column is NOT NULL
- `active` column is NOT NULL with default `true`

### Entity Relationships

- `Routine` has a `@ManyToOne` relationship to `User` (lazy)
- `Routine` has a `@OneToMany` relationship to `RoutineExercise` (cascade all, orphan removal, lazy)
- `RoutineExercise` has a `@ManyToOne` relationship to `Routine` (lazy)
- `RoutineExercise` has a `@ManyToOne` relationship to `Exercise` (lazy)
- Exercises are ordered by `position` ascending within each routine

## Error Handling

Common errors and their causes:
- **ResourceAlreadyExistsException** – Routine name already exists for the user
- **ResourceNotFoundException** – Routine not found or user lacks access
- **ValidationException** – Invalid exercise data for the exercise category
- **BadRequestException** – Malformed request or missing required fields

## Ordering

Exercises are ordered by position in ascending order. The repository query uses `@OrderBy("position ASC")` to ensure consistent ordering. The `toResponseDTO` method sorts exercises by position before mapping.

## Ownership

All operations are user-scoped:
- The authenticated user's ID is extracted from `UserDetailsImpl` (via `@AuthenticationPrincipal`)
- Routines can only be created, read, updated, and deleted by their owner
- Delete operations check `findByIdAndActiveTrue` and validate ownership before deactivation