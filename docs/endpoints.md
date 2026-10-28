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

`UserResponse` includes `profilePictureUrl`, a temporary URL for the processed
profile picture, or `null` when the user has not uploaded one.

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
| GET | `/api/v1/routines` | Required | None | `Page<RoutineListItemDTO>` | 200 |
| GET | `/api/v1/routines/{routineId}` | Required | None | `RoutineResponseDTO` | 200 |
| POST | `/api/v1/routines` | Required | `RoutineRequestDTO` | `RoutineResponseDTO` | 201 |
| PUT | `/api/v1/routines/{routineId}` | Required | `RoutineRequestDTO` | `RoutineResponseDTO` | 200 |
| DELETE | `/api/v1/routines/{routineId}` | Required | None | None | 204 |
| POST | `/api/v1/routines/{routineId}/exercises/{position}` | Required | `RoutineExerciseRequestDTO` | `RoutineResponseDTO` | 200 |
| PUT | `/api/v1/routines/{routineId}/exercises/{position}` | Required | `RoutineExerciseRequestDTO` | `RoutineResponseDTO` | 200 |
| DELETE | `/api/v1/routines/{routineId}/exercises/{position}` | Required | None | None | 204 |

---

## Workout Sessions

### Sessions

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/sessions` | Required | None | `Page<WorkoutSessionListItemDTO>` | 200 |
| GET | `/api/v1/sessions/{sessionId}` | Required | None | `WorkoutSessionResponseDTO` | 200 |
| POST | `/api/v1/sessions` | Required | `WorkoutSessionRequestDTO` | `WorkoutSessionResponseDTO` | 201 |
| POST | `/api/v1/sessions/from-routine/{routineId}` | Required | None | `WorkoutSessionResponseDTO` | 201 |
| PATCH | `/api/v1/sessions/{sessionId}/notes` | Required | `NotesUpdateRequestDTO` | None | 204 |
| PATCH | `/api/v1/sessions/{sessionId}/finish` | Required | None | `WorkoutSessionResponseDTO` | 200 |
| DELETE | `/api/v1/sessions/{sessionId}` | Required | None | None | 204 |

### Session Exercises

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| POST | `/api/v1/sessions/{sessionId}/exercises` | Required | `SessionExerciseRequestDTO` | `SessionExerciseAddedResponseDTO` | 201 |
| PATCH | `/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/notes` | Required | `NotesUpdateRequestDTO` | None | 204 |
| PATCH | `/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/position` | Required | `PositionRequestDTO` | `List<SessionExercisePositionDTO>` | 200 |
| DELETE | `/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}` | Required | None | None | 204 |

### Session Sets

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| POST | `/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/sets` | Required | `SessionSetRequestDTO` | `SessionSetResponseDTO` | 201 |
| PUT | `/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/sets/{sessionSetId}` | Required | `SessionSetRequestDTO` | `SessionSetResponseDTO` | 200 |
| DELETE | `/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/sets/{sessionSetId}` | Required | None | None | 204 |

---

## Progress Records

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/progress?from={from}&to={to}` | Required | None | `Page<ProgressRecordResponseDTO>` | 200 |
| POST | `/api/v1/progress` | Required | `ProgressRecordRequestDTO` | `ProgressRecordResponseDTO` | 201 |
| DELETE | `/api/v1/progress/{id}` | Required | None | None | 204 |

---

## Statistics

### Session Volume

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/stats/volume?sessionId={sessionId}` | Required | None | `SessionVolumeDTO` | 200 |
| GET | `/api/v1/stats/volume?from={from}&to={to}` | Required | None | `SessionVolumeDTO` | 200 |

`/volume` has two variants: one scoped to a single session, and one for a
date range when `from` and `to` are supplied.

### Monthly Volume

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/stats/volume/monthly?from={from}&to={to}` | Required | None | `List<MonthlyVolumeDTO>` | 200 |

### Estimated 1RM

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/stats/1rm?exerciseId={exerciseId}` | Required | None | `OneRepMaxDTO` | 200 |

### Active Streak

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/stats/streak` | Required | None | `StreakDTO` | 200 |

### Exercise Progression

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/stats/progress?exerciseId={exerciseId}&from={from}&to={to}` | Required | None | `ExerciseProgressPointsDTO` | 200 |

### Muscle Intensity

| Método | Endpoint | Auth | Request | Response | Status |
| ------ | -------- | ---- | ------- | -------- | ------ |
| GET | `/api/v1/stats/muscle-intensity?from={from}&to={to}` | Required | None | `MuscleIntensityResponseDTO` | 200 |
