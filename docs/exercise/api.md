# API Reference

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/exercises` | List exercises with filtering and pagination |
| GET | `/api/v1/exercises/{slug}` | Get exercise details by slug |
| POST | `/api/v1/exercises` | Create exercise (ADMIN only) |
| PUT | `/api/v1/exercises/{slug}` | Fully update exercise (ADMIN only) |
| DELETE | `/api/v1/exercises/{slug}` | Soft-delete exercise (ADMIN only) |
| GET | `/api/v1/muscles` | List all muscles (paginated) |
| GET | `/api/v1/muscles/{slug}` | Get muscle by slug |

All endpoints require authentication via Bearer token.

## List Exercises (`GET /api/v1/exercises`)

**Description:** Retrieves a paginated list of active exercises with optional filtering.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| cursor | UUID | No | Pagination cursor (last ID from previous page) |
| size | Integer | No | Number of items to return (max 100) |
| category | Category | No | Filter by exercise category |
| difficulty | Difficulty | No | Filter by difficulty level |
| muscleSlugs | List[String] | No | Filter by one or more muscle slugs |

**Validation:**
- `size` – maximum 100, must be positive
- `cursor` – must be a valid UUID

**Success Response (200):**
```json
{
  "page": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "Bicep Curl",
      "slug": "bicep-curl",
      "category": "STRENGTH",
      "difficulty": "INTERMEDIATE"
    }
  ]
}
```

**Error Responses:**
- `400` – Invalid request parameters (e.g., size > 100)
- `401` – Unauthorized
- `500` – Internal server error

## Get Exercise (`GET /api/v1/exercises/{slug}`)

**Description:** Retrieves a single active exercise by its slug, including muscle relationships.

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| slug | String | Yes | URL-friendly exercise identifier (e.g., "bicep-curl") |

**Validation:**
- `slug` – required, not blank

**Success Response (200):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Bicep Curl",
  "slug": "bicep-curl",
  "description": "Made with a bicep curl bar",
  "instructions": [
    "Stand with your feet shoulder-width apart.",
    "Curl the bar towards your shoulders."
  ],
  "category": "STRENGTH",
  "difficulty": "INTERMEDIATE",
  "exerciseMuscles": [
    {
      "muscle": { "name": "bicep", "slug": "bicep", "bodyRegion": "ARMS" },
      "impactLevel": "PRIMARY"
    }
  ],
  "gifUrl": null
}
```

**Response fields:**
- `instructions` – ordered list of step-by-step execution steps; empty array when the exercise has no steps.
- `gifUrl` – public URL of the exercise demonstration GIF. Currently always `null` (placeholder); URL resolution (signed or public) from the internal storage key will be implemented later in the service layer.
- `media_object_key` (internal object-storage key, e.g. `exercises/abc123.gif`) is **never exposed** by the API. It is an infrastructure detail, not a client-facing field.
- `impactLevel` – one of `PRIMARY`, `SECONDARY`, `STABILIZER`. Seed data populates all three levels.

**Error Responses:**
- `400` – Slug is blank or null
- `401` – Unauthorized
- `404` – Exercise not found
- `500` – Internal server error

## Create Exercise (`POST /api/v1/exercises`)

**Description:** Creates a new exercise with optional muscle associations. Requires `ADMIN` role.

**Request Body (`ExerciseRequestDTO`):**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | Yes | Exercise name (not blank) |
| slug | String | Yes | URL-friendly identifier, unique across active and inactive exercises |
| description | String | Yes | Human-readable description (not blank) |
| instructions | List[String] | No | Ordered step-by-step execution steps; omitted or null defaults to an empty list |
| category | Category | Yes | `STRENGTH`, `CARDIO` or `MOBILITY` |
| difficulty | Difficulty | Yes | `BEGINNER`, `INTERMEDIATE` or `ADVANCED` |
| muscles | List | No | Muscle associations (`muscleSlug` + `impactLevel`); each slug must reference an existing muscle, without duplicates |

**Success Response (201):** `ExerciseDetailDTO` of the created exercise.

**Error Responses:**
- `400` – Invalid request body (blank name/slug/description, null category/difficulty)
- `401` – Unauthorized
- `403` – Forbidden: ADMIN role required
- `404` – Referenced muscle slug does not exist
- `409` – Exercise slug already exists
- `500` – Internal server error

## Update Exercise (`PUT /api/v1/exercises/{slug}`)

**Description:** Fully updates the active exercise identified by slug, replacing its muscle associations with the supplied ones. Requires `ADMIN` role. The slug itself may be changed as long as the new value is not taken.

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| slug | String | Yes | URL-friendly exercise identifier of the exercise to update |

**Request Body:** same `ExerciseRequestDTO` as create.

**Success Response (200):** `ExerciseDetailDTO` of the updated exercise.

**Error Responses:**
- `400` – Invalid slug or request body
- `401` – Unauthorized
- `403` – Forbidden: ADMIN role required
- `404` – Exercise or referenced muscle not found
- `409` – New slug already used by another exercise
- `500` – Internal server error

## Delete Exercise (`DELETE /api/v1/exercises/{slug}`)

**Description:** Soft-deletes the active exercise: it is kept with `active = false` and excluded from all reads. Requires `ADMIN` role.

**Success Response:** `204 No Content` (empty body).

**Error Responses:**
- `400` – Slug is blank or null
- `401` – Unauthorized
- `403` – Forbidden: ADMIN role required
- `404` – Exercise not found
- `500` – Internal server error

## List Muscles (`GET /api/v1/muscles`)

**Description:** Returns a paginated list of all muscles.

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
      "name": "bicep",
      "slug": "bicep",
      "bodyRegion": "ARMS"
    }
  ]
}
```

**Error Responses:**
- `401` – Unauthorized
- `500` – Internal server error

## Get Muscle (`GET /api/v1/muscles/{slug}`)

**Description:** Returns a muscle by its slug.

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| slug | String | Yes | URL-friendly muscle identifier |

**Success Response (200):**
```json
{
  "name": "bicep",
  "slug": "bicep",
  "bodyRegion": "ARMS"
}
```

**Error Responses:**
- `401` – Unauthorized
- `404` – Muscle not found
- `500` – Internal server error

## Swagger/OpenAPI

The OpenAPI specification is defined in `OpenApiConfig.java` and specifies:
- Base path: `/api/v1/exercises` and `/api/v1/muscles`
- Security scheme: `Bearer Authentication` (JWT)
- All endpoints require authentication
- Exercise endpoints require JWT with user role
