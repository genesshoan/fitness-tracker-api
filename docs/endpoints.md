# Endpoints REST

## Base

Controllers expose with prefix **`/api/v1`**.

## Roles

'ROLE_ADMIN', 'ROLE_USER'.

### Auth

Endpoints marked as **Public** do not require a bearer token. The session endpoints
read the refresh token from the `refresh_token` cookie.

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| POST | `/api/v1/auth/register` | Public | `RegisterRequest` | `MessageResponse` | 201 |
| POST | `/api/v1/auth/verify-email` | Public | `VerifyEmailRequest` | None | 204 |
| POST | `/api/v1/auth/resend-verification` | Public | `ResendVerificationEmailRequest` | None | 204 |
| POST | `/api/v1/auth/login` | Public | `LoginRequest` | `TokenResponse` | 200 |
| POST | `/api/v1/auth/refresh` | Public | `refresh_token` cookie | `TokenResponse` | 200 |
| DELETE | `/api/v1/auth/logout` | Public | `refresh_token` cookie | None | 204 |
| POST | `/api/v1/auth/forgot-password` | Public | `ForgotPasswordRequest` | None | 204 |
| POST | `/api/v1/auth/reset-password` | Public | `ResetPasswordRequest` | None | 204 |
| PATCH | `/api/v1/auth/password` | Required | `PasswordChangeRequest` | None | 204 |

`TokenResponse` carries the access token in the body; the refresh token is set in the
`refresh_token` cookie.

`register`, `resend-verification`, `forgot-password` and `login` are rate limited
per IP and per email, and return `429` with a `Retry-After` header when the quota
is exhausted. See [Rate Limiting](auth/api.md#rate-limiting) for the limits.

---

## User

### Profile

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/user/me` | Required | None | `UserResponse` | 200 |

`UserResponse` includes `createdAt`, the account creation timestamp, and
`profilePictureUrl`, a temporary URL for the processed profile picture, or `null`
when the user has not uploaded one.

### Profile picture

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| PUT | `/api/v1/user/me/profile-picture` | Required | `multipart/form-data` (`file`) | `ProfilePictureResponse` | 200 |

The same `PUT` operation handles both the first upload and replacement. Images
are validated, cropped to the configured output size, stored under a stable
user-specific key, and returned through a temporary presigned URL.

### Username

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| PUT | `/api/v1/user/me/username` | Required | `ChangeUsernameRequest` | None | 204 |

Password changes live in the `auth` module (`PATCH /api/v1/auth/password`).

---

## Muscles

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/muscles` | Required | None | `Page<MuscleResponse>` | 200 |
| GET | `/api/v1/muscles/{slug}` | Required | None | `MuscleResponse` | 200 |

## Exercises

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/exercises` | Required | None | `CursorPage<ExerciseListItem>` | 200 |
| GET | `/api/v1/exercises/search` | Required | None | `ExerciseSearchResponse` | 200 |
| GET | `/api/v1/exercises/{slug}` | Required | None | `ExerciseDetail` | 200 |
| POST | `/api/v1/exercises` | ADMIN | `ExerciseRequest` | `ExerciseDetail` | 201 |
| PUT | `/api/v1/exercises/{slug}` | ADMIN | `ExerciseRequest` | `ExerciseDetail` | 200 |
| DELETE | `/api/v1/exercises/{slug}` | ADMIN | None | None | 204 |

---

## Routines

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/routines` | Required | None | `Page<RoutineListItem>` | 200 |
| GET | `/api/v1/routines/{routineId}` | Required | None | `RoutineResponse` | 200 |
| POST | `/api/v1/routines` | Required | `RoutineRequest` | `RoutineResponse` | 201 |
| PUT | `/api/v1/routines/{routineId}` | Required | `RoutineRequest` | `RoutineResponse` | 200 |
| DELETE | `/api/v1/routines/{routineId}` | Required | None | None | 204 |
| POST | `/api/v1/routines/{routineId}/exercises/{position}` | Required | `RoutineExerciseRequest` | `RoutineResponse` | 200 |
| PUT | `/api/v1/routines/{routineId}/exercises/{position}` | Required | `RoutineExerciseRequest` | `RoutineResponse` | 200 |
| DELETE | `/api/v1/routines/{routineId}/exercises/{position}` | Required | None | None | 204 |

---

## Workout Sessions

### Sessions

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/sessions` | Required | None | `Page<WorkoutSessionListItem>` | 200 |
| GET | `/api/v1/sessions/{sessionId}` | Required | None | `WorkoutSessionResponse` | 200 |
| POST | `/api/v1/sessions` | Required | `WorkoutSessionRequest` | `WorkoutSessionResponse` | 201 |
| POST | `/api/v1/sessions/from-routine/{routineId}` | Required | None | `WorkoutSessionResponse` | 201 |
| PATCH | `/api/v1/sessions/{sessionId}/notes` | Required | `NotesUpdateRequest` | None | 204 |
| PATCH | `/api/v1/sessions/{sessionId}/finish` | Required | None | `WorkoutSessionResponse` | 200 |
| DELETE | `/api/v1/sessions/{sessionId}` | Required | None | None | 204 |

### Session Exercises

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| POST | `/api/v1/sessions/{sessionId}/exercises` | Required | `SessionExerciseRequest` | `SessionExerciseAddedResponse` | 201 |
| PATCH | `/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/notes` | Required | `NotesUpdateRequest` | None | 204 |
| PATCH | `/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/position` | Required | `PositionRequest` | `List<SessionExercisePosition>` | 200 |
| DELETE | `/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}` | Required | None | None | 204 |

### Session Sets

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| POST | `/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/sets` | Required | `SessionSetRequest` | `SessionSetResponse` | 201 |
| PUT | `/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/sets/{sessionSetId}` | Required | `SessionSetRequest` | `SessionSetResponse` | 200 |
| PATCH | `/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/sets/{sessionSetId}/complete` | Complete set |
| PATCH | `/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/sets/{sessionSetId}/incomplete` | Mark set as incomplete |
| PATCH | `/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/sets/{sessionSetId}/incomplete` | Required | None | `SessionSetResponse` | 200 |
| DELETE | `/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/sets/{sessionSetId}` | Required | None | None | 204 |

---

## Progress Records

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/progress?from={from}&to={to}` | Required | None | `Page<ProgressRecordResponse>` | 200 |
| POST | `/api/v1/progress` | Required | `ProgressRecordRequest` | `ProgressRecordResponse` | 201 |
| DELETE | `/api/v1/progress/{id}` | Required | None | None | 204 |

---

## Statistics

### Session Volume

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/stats/volume?sessionId={sessionId}` | Required | None | `SessionVolume` | 200 |
| GET | `/api/v1/stats/volume?from={from}&to={to}` | Required | None | `SessionVolume` | 200 |

`/volume` has two variants: one scoped to a single session, and one for a
date range when `from` and `to` are supplied.

### Monthly Volume

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/stats/volume/monthly?from={from}&to={to}` | Required | None | `List<MonthlyVolume>` | 200 |

### Estimated 1RM

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/stats/1rm?exerciseId={exerciseId}` | Required | None | `OneRepMax` | 200 |

### Active Streak

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/stats/streak` | Required | None | `Streak` | 200 |

### Exercise Progression

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/stats/progress?exerciseId={exerciseId}&from={from}&to={to}` | Required | None | `ExerciseProgressPoints` | 200 |

### Muscle Intensity

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/stats/muscle-intensity?from={from}&to={to}` | Required | None | `MuscleIntensityResponse` | 200 |

---

## Achievements

### Catalog

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/achievements` | Required | None | `List<AchievementResponse>` | 200 |
| GET | `/api/v1/achievements/{code}` | Required | None | `List<AchievementResponse>` | 200 |

The catalog accepts the optional filters `category`, `unlockedOnly`, and
`unseenOnly`. The detail endpoint returns a list because an achievement scoped to
an exercise has one entry per exercise the user earned it on.

### Feed

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/achievements/feed` | Required | None | `List<AchievementFeedResponse>` | 200 |
| POST | `/api/v1/achievements/seen` | Required | `MarkAchievementsSeenRequest` | None | 204 |

The feed accepts the optional filters `unseenOnly` and `limit`.

### Summary

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/achievements/summary` | Required | None | `AchievementSummaryResponse` | 200 |
