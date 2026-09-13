# API Reference

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/exercises` | List exercises with filtering and pagination |
| GET | `/api/v1/exercises/{slug}` | Get exercise details by slug |
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
  "category": "STRENGTH",
  "difficulty": "INTERMEDIATE",
  "exerciseMuscles": [
    {
      "muscle": { "name": "bicep", "slug": "bicep", "bodyRegion": "ARMS" },
      "impactLevel": "PRIMARY"
    }
  ]
}
```

**Error Responses:**
- `400` – Slug is blank or null
- `401` – Unauthorized
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
