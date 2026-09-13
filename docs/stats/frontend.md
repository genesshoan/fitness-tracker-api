# Frontend Integration

Use the statistics endpoints for dashboard cards, progress charts, session summaries, and streak displays.

| UI feature | Endpoint | Notes |
|---|---|---|
| Session volume | `/api/v1/stats/volume?sessionId=...` | Completed strength sets only |
| Exercise 1RM | `/api/v1/stats/1rm?exerciseId=...` | Returns zero when no qualifying history exists |
| Streak card | `/api/v1/stats/streak` | Uses the user's configured timezone |
| Progress chart | `/api/v1/stats/progress?...` | `from` inclusive, `to` exclusive |

Do not assume one chart point per set: progression collapses each completed session to its best estimated 1RM set. A response can contain an empty `progress` list for a valid exercise and range.

Handle `401` by re-authenticating, `404` for unknown or inaccessible resources, and range validation errors without retrying unchanged parameters.
