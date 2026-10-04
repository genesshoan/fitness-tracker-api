# API Reference

All endpoints require an authenticated user and a JWT bearer token. Exercise write endpoints additionally require the `ADMIN` role.

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

## List Exercises (`GET /api/v1/exercises`)

Returns active exercises using cursor pagination and optional filters.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| cursor | UUID | No | Last ID from the previous page |
| size | Integer | No | Number of items; positive, maximum 100 |
| category | Category | No | `STRENGTH`, `CARDIO`, or `MOBILITY` |
| difficulty | Difficulty | No | `BEGINNER`, `INTERMEDIATE`, or `ADVANCED` |
| muscleSlugs | List[String] | No | Exercises associated with any supplied muscle slug |

`cursor` must be a valid UUID. A successful response is `200 OK` with a cursor page of exercise list items.

## Get Exercise (`GET /api/v1/exercises/{slug}`)

Returns an active exercise and its muscle relationships. `slug` is required and must not be blank.

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
      "muscle": { "name": "Biceps Brachii", "slug": "biceps", "bodyRegion": "ARMS" },
      "impactLevel": "PRIMARY"
    }
  ],
  "gifUrl": null
}
```

`instructions` is an ordered array and defaults to an empty array. `gifUrl` is currently always `null`. The nullable internal `media_object_key` is never exposed and is assigned later by the media pipeline.

## Create Exercise (`POST /api/v1/exercises`)

Creates an exercise with optional muscle associations. Requires `ADMIN`.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | Yes | Non-blank exercise name |
| slug | String | Yes | Unique URL-friendly identifier |
| description | String | Yes | Non-blank description |
| instructions | List[String] | No | Ordered steps; null defaults to `[]` |
| category | Category | Yes | `STRENGTH`, `CARDIO`, or `MOBILITY` |
| difficulty | Difficulty | Yes | `BEGINNER`, `INTERMEDIATE`, or `ADVANCED` |
| muscles | List | No | `muscleSlug` and `impactLevel`; no duplicate slugs |

Returns `201 Created` with `ExerciseDetailDTO`.

## Update Exercise (`PUT /api/v1/exercises/{slug}`)

Fully updates the active exercise identified by the path slug. The request body is the same as create; the submitted muscle list replaces all existing associations. The slug may change if the new slug is unused. Returns `200 OK`.

## Delete Exercise (`DELETE /api/v1/exercises/{slug}`)

Soft-deletes the active exercise by setting `active = false`; it is excluded from reads and its slug remains reserved. Returns `204 No Content`.

## List Muscles (`GET /api/v1/muscles`)

Returns a paginated list of muscles. Use the exact slugs listed in [Muscle slugs](README.md#muscle-slugs) when filtering exercises or submitting muscle associations.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| page | Integer | No | Zero-based page number |
| size | Integer | No | Number of items per page |
| sort | String | No | Sort criteria |

Returns `200 OK` with a Spring `Page<MuscleResponseDTO>`.

## Get Muscle (`GET /api/v1/muscles/{slug}`)

Returns a muscle by its URL-friendly slug. Returns `200 OK`, or `404 Not Found` when the slug does not exist.

## Common Error Responses

| Status | Meaning |
|--------|---------|
| 400 | Invalid request parameter or body |
| 401 | Missing or invalid JWT |
| 403 | Authenticated user lacks `ADMIN` for a write operation |
| 404 | Exercise or muscle not found |
| 409 | Exercise slug already exists |
| 500 | Internal server error |

## Swagger/OpenAPI

The OpenAPI specification is defined in `OpenApiConfig.java`:

- Base paths: `/api/v1/exercises` and `/api/v1/muscles`
- Security scheme: JWT bearer token
- All endpoints require authentication
- Exercise write endpoints require the `ADMIN` role
