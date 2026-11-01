# API Reference

Base path: `/api/v1/achievements`. Every endpoint is scoped to the
authenticated user.

## Response Shapes

### Achievement

One entry of the catalog, with the state the user reached in it.

| Field | Type | Notes |
|---|---|---|
| `code` | string | Stable key of the achievement, for example `WORKOUTS_10` |
| `name` | string | Display name |
| `description` | string | What the user has to do to earn it |
| `category` | string | `MILESTONE`, `CONSISTENCY`, `VOLUME`, `EXPLORATION`, `PERSONAL_RECORD` |
| `tier` | string | `BRONZE`, `SILVER`, `GOLD` |
| `ruleType` | string | `THRESHOLD` or `PERSONAL_RECORD` |
| `metric` | string | The metric the achievement is measured against |
| `threshold` | number | The value the metric has to reach, `null` for a record rule |
| `scope` | string | `GLOBAL` or `PER_EXERCISE` |
| `iconKey` | string | Key of the image that represents the achievement, `null` when there is none |
| `userAchievementId` | string | Identifier of the state of the user, `null` when the user has none yet |
| `exerciseId` | string | The exercise the achievement is scoped to, `null` for a global achievement |
| `exerciseName` | string | Name of that exercise, `null` for a global achievement |
| `progressValue` | number | Best value reached towards the threshold, `null` for a record rule |
| `progressRatio` | number | Progress towards the threshold between 0 and 1, `null` when there is no progress |
| `currentValue` | number | Current record of the scoped exercise, `null` for a global achievement |
| `unlocked` | boolean | Whether the achievement has been unlocked |
| `unlockedAt` | string | Instant of the unlock, `null` while it is locked |
| `seen` | boolean | Whether the user has seen the unlock |

### Feed entry

| Field | Type | Notes |
|---|---|---|
| `id` | string | Identifier of the state, the one to send back to mark it as seen |
| `code`, `name`, `tier`, `category` | string | The achievement |
| `exerciseId`, `exerciseName` | string | The exercise the achievement is scoped to, `null` for a global one |
| `unlockedAt` | string | Instant of the unlock |
| `seen` | boolean | Whether the user has seen the unlock |

### Summary

| Field | Type | Notes |
|---|---|---|
| `totalAvailable` | number | Achievements in the catalog |
| `totalUnlocked` | number | Achievements the user unlocked |
| `unseenCount` | number | Unlocks the user has not seen yet |
| `byCategory` | object | Unlocked and total counts per category |
| `next` | array | The achievements the user is closest to unlocking, with `code`, `name`, `progressValue`, `threshold`, and `remaining` |

## Endpoints

### `GET /api/v1/achievements`

The catalog with the state of the user.

| Query | Type | Notes |
|---|---|---|
| `category` | string | Keeps only one category |
| `unlockedOnly` | boolean | Keeps only the achievements the user unlocked |
| `unseenOnly` | boolean | Keeps only the unlocks the user has not seen yet |

Returns `200` with a list of [Achievement](#achievement).

### `GET /api/v1/achievements/{code}`

The entries of one achievement.

Returns `200` with a list of [Achievement](#achievement): one element for a
global achievement, and one per exercise for an achievement scoped to an
exercise. Returns `404` when the code does not exist or is not offered.

### `GET /api/v1/achievements/feed`

The achievements the user unlocked, most recent first.

| Query | Type | Notes |
|---|---|---|
| `unseenOnly` | boolean | Keeps only the unlocks the user has not seen yet |
| `limit` | number | Maximum number of entries, `50` by default |

Returns `200` with a list of [Feed entry](#feed-entry).

### `GET /api/v1/achievements/summary`

The state of the collection of the user.

Returns `200` with a [Summary](#summary).

### `POST /api/v1/achievements/seen`

Marks the given unlocks as seen, so the client stops showing them as new.

```json
{ "achievementIds": ["9b2e...", "1f77..."] }
```

Returns `204` with no body. The operation is idempotent, and it only affects the
unlocks of the authenticated user.

## Errors

| Status | When |
|---|---|
| `400` | The body of `seen` is missing the identifiers |
| `401` | The request is not authenticated |
| `404` | The code does not exist or is not offered |
