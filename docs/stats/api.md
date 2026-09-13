# API Reference

All endpoints require JWT authentication and scope results to the authenticated user.

| Method | Path | Query parameters | Response |
|---|---|---|---|
| GET | `/api/v1/stats/volume` | `sessionId` (UUID) | `SessionVolumeDTO` |
| GET | `/api/v1/stats/1rm` | `exerciseId` (UUID) | `OneRepMaxDTO` |
| GET | `/api/v1/stats/streak` | none | `StreakDTO` |
| GET | `/api/v1/stats/progress` | `exerciseId`, `from`, `to` | `ExerciseProgressPointsDTO` |

`from` is inclusive and `to` is exclusive. Both use ISO-8601 instants.

## Response Shapes

```json
{"volumeKg": 4320.0}
```

```json
{"exerciseId":"123e4567-e89b-12d3-a456-426614174000","estimatedOneRepMax":101.3}
```

```json
{"currentStreak":5,"longestStreak":12,"lastActiveDay":"2026-09-12"}
```

```json
{"exerciseId":"123e4567-e89b-12d3-a456-426614174000","progress":[
  {"date":"2026-09-12","weightKg":80.0,"reps":8,"estimatedOneRepMax":101.3}
]}
```

Errors use `application/problem+json`; invalid ranges and missing resources are reported as `400`/`404` according to the controller contract.
