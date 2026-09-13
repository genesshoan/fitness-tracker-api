# API Reference

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/progress` | Get progress records within a date range |
| POST | `/api/v1/progress` | Create a progress record |
| DELETE | `/api/v1/progress/{id}` | Delete a progress record |

All endpoints require authentication via Bearer token. The authenticated user's ID is extracted from `UserDetailsImpl`.

## Get Progress Records (`GET /api/v1/progress`)

**Description:** Retrieves progress records for the authenticated user within a date range.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| from | LocalDate | Yes | Start date (inclusive) |
| to | LocalDate | Yes | End date (inclusive) |
| page | Integer | No | Page number (0-based) |
| size | Integer | No | Number of items per page |
| sort | String | No | Sort criteria |

**Validation:**
- `from` – required, must not be after `to`
- `to` – required

**Success Response (200):**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "recordedAt": "2026-08-25",
      "weightKg": 75.5,
      "bodyFatPercentage": 20.0
    }
  ]
}
```

**Error Responses:**
- `400` – Invalid date range (from > to)
- `401` – Unauthorized
- `500` – Internal server error

## Create Progress Record (`POST /api/v1/progress`)

**Description:** Creates a new progress record for the authenticated user.

**Request Body:**
```json
{
  "recordedAt": "2026-08-25",
  "weightKg": 75.5,
  "bodyFatPercentage": 20.0
}
```

**Validation:**
- `recordedAt` – required, must be today or in the past
- `weightKg` – required, must be positive, max 500
- `bodyFatPercentage` – optional, must be between 0 and 100

**Success Response (201):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "recordedAt": "2026-08-25",
  "weightKg": 75.5,
  "bodyFatPercentage": 20.0
}
```

**Error Responses:**
- `400` – Invalid record data (validation failure)
- `401` – Unauthorized
- `409` – A progress record already exists for this date
- `500` – Internal server error

## Delete Progress Record (`DELETE /api/v1/progress/{id}`)

**Description:** Deletes a progress record belonging to the authenticated user.

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | UUID | Yes | The progress record ID |

**Success Response (204):**
```
```

**Error Responses:**
- `400` – Invalid progress record ID
- `401` – Unauthorized
- `404` – Progress record not found (or does not belong to user)
- `500` – Internal server error

## Swagger/OpenAPI

The OpenAPI specification is defined in `OpenApiConfig.java` and specifies:
- Base path: `/api/v1/progress`
- Security scheme: `Bearer Authentication` (JWT)
- All endpoints require authentication
- User ownership is enforced at the service level
