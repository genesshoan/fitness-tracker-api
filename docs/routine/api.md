# API Reference

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/routines` | List routines with pagination |
| GET | `/api/v1/routines/{routineId}` | Get routine details by ID |
| POST | `/api/v1/routines` | Create a new routine |
| PUT | `/api/v1/routines/{routineId}` | Update an existing routine |
| DELETE | `/api/v1/routines/{routineId}` | Soft delete a routine |
| POST | `/api/v1/routines/{routineId}/exercises/{position}` | Add an exercise to a routine |
| PUT | `/api/v1/routines/{routineId}/exercises/{position}` | Update an exercise in a routine |
| DELETE | `/api/v1/routines/{routineId}/exercises/{position}` | Remove an exercise from a routine |

All endpoints require authentication via Bearer token. The authenticated user's ID is extracted from `UserDetailsImpl`.

## List Routines (`GET /api/v1/routines`)

**Description:** Retrieves a paginated list of active routines for the authenticated user, including exercise counts.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| page | Integer | No | Page number (0-based) |
| size | Integer | No | Number of items per page |
| sort | String | No | Sort criteria |

**Success Response (200):**
```json
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "Push Day",
      "exerciseCount": 5,
      "updatedAt": "2026-09-13T10:30:00"
    }
  ],
  "totalPages": 2,
  "totalElements": 10,
  "page": 0,
  "size": 20
}
```

**Error Responses:**
- `401` – Unauthorized
- `500` – Internal server error

## Get Routine (`GET /api/v1/routines/{routineId}`)

**Description:** Retrieves a single routine by its ID, including all exercises with their metrics.

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| routineId | UUID | Yes | The routine ID |

**Success Response (200):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Push Day",
  "description": "A routine for pushing heavy weights",
  "exercises": [
    {
      "exercise": {
        "id": "123e4567-e89b-12d3-a456-426614174001",
        "name": "Bench Press",
        "slug": "bench-press",
        "category": "STRENGTH",
        "difficulty": "INTERMEDIATE"
      },
      "position": 1,
      "defaultRestSeconds": 60,
      "defaultSets": 3,
      "defaultReps": 10,
      "defaultWeightKg": 100.0,
      "defaultDurationSeconds": 0,
      "defaultDistanceKm": 0.0,
      "notes": ""
    }
  ]
}
```

**Error Responses:**
- `401` – Unauthorized
- `404` – Routine not found (or does not belong to user)
- `500` – Internal server error

## Create Routine (`POST /api/v1/routines`)

**Description:** Creates a new routine for the authenticated user with optional initial exercises.

**Request Body:**
```json
{
  "name": "Push Day",
  "description": "A routine for pushing heavy weights",
  "exercises": [
    {
      "exerciseId": "123e4567-e89b-12d3-a456-426614174001",
      "defaultRestSeconds": 60,
      "defaultSets": 3,
      "defaultReps": 10,
      "defaultWeightKg": 100.0,
      "defaultDurationSeconds": 0,
      "defaultDistanceKm": 0.0,
      "notes": ""
    }
  ]
}
```

**Validation:**
- `name` – required, not blank
- `description` – optional, nullable
- `exercises` – optional, list of exercise requests
- `exerciseId` – required, valid UUID
- `defaultRestSeconds` – required, must be non-negative (>= 0)
- `defaultSets` – required, must be positive (> 0)
- `defaultReps`, `defaultWeightKg`, `defaultDurationSeconds`, and `defaultDistanceKm` – optional and non-negative when provided
- Exercise metrics must match the referenced exercise category:
  - `STRENGTH` requires positive reps and weight, with no duration or distance
  - `CARDIO` requires a positive duration or distance, with no reps or weight
  - `MOBILITY` requires a positive duration, with no reps, weight, or distance

**Success Response (201):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Push Day",
  "description": "A routine for pushing heavy weights",
  "exercises": [
    {
      "exercise": {
        "id": "123e4567-e89b-12d3-a456-426614174001",
        "name": "Bench Press",
        "slug": "bench-press",
        "category": "STRENGTH",
        "difficulty": "INTERMEDIATE"
      },
      "position": 1,
      "defaultRestSeconds": 60,
      "defaultSets": 3,
      "defaultReps": 10,
      "defaultWeightKg": 100.0,
      "defaultDurationSeconds": 0,
      "defaultDistanceKm": 0.0,
      "notes": ""
    }
  ]
}
```

**Error Responses:**
- `400` – Invalid request data (missing fields, wrong format)
- `401` – Unauthorized
- `409` – Routine with the same name already exists
- `500` – Internal server error

## Update Routine (`PUT /api/v1/routines/{routineId}`)

**Description:** Updates the name and description of an existing routine. Replaces all exercises with the provided list.

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| routineId | UUID | Yes | The routine ID |

**Request Body:**
```json
{
  "name": "Push Day",
  "description": "Updated description",
  "exercises": [
    {
      "exerciseId": "123e4567-e89b-12d3-a456-426614174001",
      "defaultRestSeconds": 60,
      "defaultSets": 3,
      "defaultReps": 10,
      "defaultWeightKg": 100.0,
      "defaultDurationSeconds": 0,
      "defaultDistanceKm": 0.0,
      "notes": ""
    }
  ]
}
```

**Validation:** Same as Create Routine, plus `routineId` must reference an existing routine owned by the authenticated user.

**Success Response (200):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Push Day",
  "description": "Updated description",
  "exercises": [
    {
      "exercise": {
        "id": "123e4567-e89b-12d3-a456-426614174001",
        "name": "Bench Press",
        "slug": "bench-press",
        "category": "STRENGTH",
        "difficulty": "INTERMEDIATE"
      },
      "position": 1,
      "defaultRestSeconds": 60,
      "defaultSets": 3,
      "defaultReps": 10,
      "defaultWeightKg": 100.0,
      "defaultDurationSeconds": 0,
      "defaultDistanceKm": 0.0,
      "notes": ""
    }
  ]
}
```

**Error Responses:**
- `400` – Invalid request data (missing fields, wrong format)
- `401` – Unauthorized
- `404` – Routine not found
- `409` – Routine with the same name already exists
- `500` – Internal server error

## Delete Routine (`DELETE /api/v1/routines/{routineId}`)

**Description:** Soft deletes a routine by setting its `active` flag to false.

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| routineId | UUID | Yes | The routine ID |

**Success Response (204):**
```
```

**Error Responses:**
- `401` – Unauthorized
- `404` – Routine not found
- `500` – Internal server error

## Add Exercise to Routine (`POST /api/v1/routines/{routineId}/exercises/{position}`)

**Description:** Adds an exercise at a specific position in the routine. Exercises at or after the specified position are shifted down by 1.

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| routineId | UUID | Yes | The routine ID |
| position | Integer | Yes | Position to insert the exercise at (1-based, minimum 1) |

**Request Body:**
```json
{
  "exerciseId": "123e4567-e89b-12d3-a456-426614174001",
  "defaultRestSeconds": 60,
  "defaultSets": 3,
  "defaultReps": 10,
  "defaultWeightKg": 100.0,
  "defaultDurationSeconds": 0,
  "defaultDistanceKm": 0.0,
  "notes": ""
}
```

**Validation:**
- `position` – required, must be >= 1
- `exerciseId` – required, valid UUID
- Exercise metrics must be valid for the exercise category

**Success Response (200):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Push Day",
  "exercises": [
    {
      "exercise": {
        "id": "123e4567-e89b-12d3-a456-426614174001",
        "name": "Bench Press",
        "slug": "bench-press",
        "category": "STRENGTH",
        "difficulty": "INTERMEDIATE"
      },
      "position": 1,
      "defaultRestSeconds": 60,
      "defaultSets": 3,
      "defaultReps": 10,
      "defaultWeightKg": 100.0,
      "defaultDurationSeconds": 0,
      "defaultDistanceKm": 0.0,
      "notes": ""
    }
  ]
}
```

**Error Responses:**
- `400` – Invalid request data (position < 1)
- `401` – Unauthorized
- `404` – Routine or exercise not found
- `500` – Internal server error

The authenticated user must own the routine. Inactive routines are treated as not found.

## Update Exercise in Routine (`PUT /api/v1/routines/{routineId}/exercises/{position}`)

**Description:** Updates the metrics for an exercise at a specific position in the routine.

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| routineId | UUID | Yes | The routine ID |
| position | Integer | Yes | Position of the exercise to update (1-based, minimum 1) |

**Request Body:**
```json
{
  "exerciseId": "123e4567-e89b-12d3-a456-426614174001",
  "defaultRestSeconds": 60,
  "defaultSets": 3,
  "defaultReps": 10,
  "defaultWeightKg": 100.0,
  "defaultDurationSeconds": 0,
  "defaultDistanceKm": 0.0,
  "notes": ""
}
```

**Validation:**
- `position` – required, must be >= 1
- `exerciseId` – required, valid UUID
- Exercise metrics must be valid for the exercise category

**Success Response (200):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Push Day",
  "exercises": [
    {
      "exercise": {
        "id": "123e4567-e89b-12d3-a456-426614174001",
        "name": "Bench Press",
        "slug": "bench-press",
        "category": "STRENGTH",
        "difficulty": "INTERMEDIATE"
      },
      "position": 1,
      "defaultRestSeconds": 60,
      "defaultSets": 3,
      "defaultReps": 10,
      "defaultWeightKg": 100.0,
      "defaultDurationSeconds": 0,
      "defaultDistanceKm": 0.0,
      "notes": ""
    }
  ]
}
```

**Error Responses:**
- `400` – Invalid request data (position < 1)
- `401` – Unauthorized
- `404` – Routine or exercise not found
- `500` – Internal server error

The authenticated user must own the routine. Inactive routines are treated as not found.

## Delete Exercise from Routine (`DELETE /api/v1/routines/{routineId}/exercises/{position}`)

**Description:** Removes an exercise at a specific position from the routine. Exercises after the removed position are shifted up by 1.

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| routineId | UUID | Yes | The routine ID |
| position | Integer | Yes | Position of the exercise to remove (1-based, minimum 1) |

**Success Response (204):**
```
```

**Error Responses:**
- `401` – Unauthorized
- `404` – Routine or exercise not found
- `500` – Internal server error

## Swagger/OpenAPI

The OpenAPI specification is defined in `OpenApiConfig.java` and specifies:
- Base path: `/api/v1/routines`
- Security scheme: `Bearer Authentication` (JWT)
- All endpoints require authentication
- User ownership is enforced at the service level