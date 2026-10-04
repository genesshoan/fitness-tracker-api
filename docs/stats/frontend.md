# Frontend Integration

Use the statistics endpoints for dashboard cards, progress charts, session summaries, and streak displays.

| UI feature | Endpoint | Notes |
|---|---|---|
| Session volume | `/api/v1/stats/volume?sessionId=...` | Completed strength sets only |
| Date-range volume | `/api/v1/stats/volume?from=...&to=...` | Inclusive local-date range in the user's timezone |
| Monthly volume chart | `/api/v1/stats/volume/monthly?from=...&to=...` | Monthly totals in the user's timezone; empty months are omitted |
| Exercise 1RM | `/api/v1/stats/1rm?exerciseId=...` | Returns zero when no qualifying history exists |
| Streak card | `/api/v1/stats/streak` | Uses the user's configured timezone |
| Progress chart | `/api/v1/stats/progress?...` | `from` inclusive, `to` exclusive |
| Muscle intensity chart | `/api/v1/stats/muscle-intensity?from=...&to=...` | Every catalog muscle, scored from 0.0 to 10.0 |

Do not assume one chart point per set: progression collapses each completed session to its best estimated 1RM set. A response can contain an empty `progress` list for a valid exercise and range.

Handle `401` by re-authenticating, `404` for unknown or inaccessible resources, and range validation errors without retrying unchanged parameters.

Monthly volume and muscle intensity accept inclusive `LocalDate` boundaries. They are interpreted in the user's configured IANA timezone. Muscle intensity weights `PRIMARY`, `SECONDARY`, and `STABILIZER` associations and is a visualization heuristic, not a physiological claim.
