# API Reference

All endpoints require JWT authentication and scope results to the authenticated user.

| Method | Path | Query parameters | Response |
|---|---|---|---|
| GET | `/api/v1/stats/volume` | `sessionId` (UUID) | `SessionVolumeDTO` |
| GET | `/api/v1/stats/volume` | `from`, `to` (`LocalDate`) | `SessionVolumeDTO` |
| GET | `/api/v1/stats/volume/monthly` | `from`, `to` (`LocalDate`) | `MonthlyVolumeDTO[]` |
| GET | `/api/v1/stats/1rm` | `exerciseId` (UUID) | `OneRepMaxDTO` |
| GET | `/api/v1/stats/streak` | none | `StreakDTO` |
| GET | `/api/v1/stats/progress` | `exerciseId`, `from`, `to` | `ExerciseProgressPointsDTO` |
| GET | `/api/v1/stats/muscle-intensity` | `from`, `to` (`LocalDate`) | `MuscleIntensityResponseDTO` |

`/volume/monthly` and `/muscle-intensity` use inclusive `LocalDate` boundaries in the user's configured timezone. Other date-range endpoints use ISO-8601 instants with inclusive `from` and exclusive `to`.

## Response Shapes

```json
{"volumeKg": 4320.0}
```

```json
[
  {"month":"2026-01","volumeKg":4320.0},
  {"month":"2026-02","volumeKg":5100.0}
]
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

```json
{
  "from": "2026-09-01",
  "to": "2026-09-30",
  "muscles": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "Biceps Brachii",
      "slug": "biceps",
      "bodyRegion": "ARMS",
      "intensity": 10.0
    }
  ]
}
```

Monthly volume includes completed strength sets with non-null weight and reps. Months without qualifying sets are omitted. Muscle intensity returns every catalog muscle, including zero-intensity muscles; its 0–10 score is a deterministic visualization heuristic relative to the strongest muscle in the selected range, not a physiological measurement.

Errors use `application/problem+json`; invalid ranges and missing resources are reported as `400`/`404` according to the controller contract.
