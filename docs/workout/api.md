# API Reference

All endpoints require JWT authentication and operate only on the authenticated user's sessions.

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/sessions` | Paginated session list |
| GET | `/api/v1/sessions/{sessionId}` | Session details |
| POST | `/api/v1/sessions` | Create from scratch |
| POST | `/api/v1/sessions/from-routine/{routineId}` | Create from active routine |
| PATCH | `/api/v1/sessions/{sessionId}/notes` | Update session notes |
| PATCH | `/api/v1/sessions/{sessionId}/finish` | Complete session |
| DELETE | `/api/v1/sessions/{sessionId}` | Delete session |
| POST | `/api/v1/sessions/{sessionId}/exercises` | Add exercise |
| PATCH | `/api/v1/sessions/{sessionId}/exercises/{exerciseId}/notes` | Update exercise notes |
| PATCH | `/api/v1/sessions/{sessionId}/exercises/{exerciseId}/position` | Move exercise |
| DELETE | `/api/v1/sessions/{sessionId}/exercises/{exerciseId}` | Delete exercise |
| POST | `/api/v1/sessions/{sessionId}/exercises/{exerciseId}/sets` | Add set |
| PUT | `/api/v1/sessions/{sessionId}/exercises/{exerciseId}/sets/{setId}` | Update set |
| DELETE | `/api/v1/sessions/{sessionId}/exercises/{exerciseId}/sets/{setId}` | Delete set |

Create requests require `status` and `startedAt`. A completed request also requires `completedAt` and at least one exercise; `completedAt` cannot be in the future. Exercise and set metrics must match the exercise category.

Successful creation returns `201`; updates return `200` or `204` as described by the endpoint; invalid mutation after completion returns `400`.
