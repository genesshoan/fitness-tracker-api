# Endpoints REST

## Base

Controllers expose with prefix **`/api/v1`**.

## Roles

'ROLE_ADMIN', 'ROLE_USER'.

### Auth

| Método | Endpoint                | Auth     | Request              | Response           | Status |
| ------ | ----------------------- | -------- | -------------------- | ------------------ | ------ |
| POST   | `/api/v1/auth/register` | No       | `RegisterRequestDTO` | `TokenResponseDTO` | 200    |
| POST   | `/api/v1/auth/login`    | No       | `LoginRequestDTO`    | `TokenResponseDTO` | 200    |
| POST   | `/api/v1/auth/refresh`  | No       | None                 | `TokenResponseDTO` | 200    |
| DELETE | `/api/v1/auth/logout`   | Required | None                 | None               | 204    |

---

## User

### Profile

| Método | Endpoint          | Auth     | Request | Response          | Status |
| ------ | ----------------- | -------- | ------- | ----------------- | ------ |
| GET    | `/api/v1/user/me` | Required | None    | `UserResponseDTO` | 200    |

### Credentials

| Método | Endpoint                   | Auth     | Request                    | Response | Status |
| ------ | -------------------------- | -------- | -------------------------- | -------- | ------ |
| PUT    | `/api/v1/user/me/password` | Required | `ChangePasswordRequestDTO` | None     | 204    |
| PUT    | `/api/v1/user/me/username` | Required | `ChangeUsernameRequestDTO` | None     | 204    |

---

## Muscles

| Método | Endpoint                 | Auth     | Request | Response                  | Status |
| ------ | ------------------------ | -------- | ------- | ------------------------- | ------ |
| GET    | `/api/v1/muscles`        | Required | None    | `Page<MuscleResponseDTO>` | 200    |
| GET    | `/api/v1/muscles/{slug}` | Required | None    | `MuscleResponseDTO`       | 200    |

## Exercises

| Método | Endpoint                 | Auth     | Request | Response                  | Status |
| ------ | ------------------------ | -------- | ------- | ------------------------- | ------ |
| GET    | `/api/v1/exercises`        | Required | None    | `CursorPage<ExerciseListItemDTO>` | 200    |
| GET    | `/api/v1/exercises/{slug}` | Required | None    | `ExerciseDetailDTO`       | 200    |

---

## Routines

| Método | Endpoint                                          | Auth     | Request                      | Response                  | Status |
| ------ | ------------------------------------------------- | -------- | ---------------------------- | ------------------------- | ------ |
| GET    | `/api/v1/routines`                                | Required | None                         | `Page<RoutineListItemDTO>` | 200    |
| GET    | `/api/v1/routines/{routineId}`                    | Required | None                         | `RoutineResponseDTO`       | 200    |
| POST   | `/api/v1/routines`                                | Required | `RoutineRequestDTO`          | `RoutineResponseDTO`       | 201    |
| PUT    | `/api/v1/routines/{routineId}`                    | Required | `RoutineRequestDTO`          | `RoutineResponseDTO`       | 200    |
| DELETE | `/api/v1/routines/{routineId}`                    | Required | None                         | None                       | 204    |
| POST   | `/api/v1/routines/{routineId}/exercises/{position}` | Required | `RoutineExerciseRequestDTO` | `RoutineResponseDTO`       | 200    |
| PUT    | `/api/v1/routines/{routineId}/exercises/{position}` | Required | `RoutineExerciseRequestDTO` | `RoutineResponseDTO`       | 200    |
| DELETE | `/api/v1/routines/{routineId}/exercises/{position}` | Required | None                         | None                       | 204    |


---

## Workout Sessions

### Sessions

| Método | Endpoint                                      | Auth     | Request                         | Response                     | Status |
| ------- | --------------------------------------------- | -------- | ------------------------------- | ---------------------------- | ------ |
| GET     | `/api/v1/sessions`                            | Required | None                            | `Page<WorkoutSessionListItemDTO>` | 200 |
| GET     | `/api/v1/sessions/{sessionId}`                | Required | None                            | `WorkoutSessionResponseDTO`  | 200 |
| POST    | `/api/v1/sessions`                            | Required | `WorkoutSessionRequestDTO`      | `WorkoutSessionResponseDTO`  | 201 |
| POST    | `/api/v1/sessions/from-routine/{routineId}`   | Required | None                            | `WorkoutSessionResponseDTO`  | 201 |
| PUT    | `/api/v1/sessions/{sessionId}/finish`         | Required | None                            | `WorkoutSessionResponseDTO`  | 200 |
| PUT    | `/api/v1/sessions/{sessionId}/cancel`         | Required | None                            | `WorkoutSessionResponseDTO`  | 200 |


### Session Exercises

| Método | Endpoint                                                | Auth     | Request                           | Response                     | Status |
| ------- | ------------------------------------------------------- | -------- | --------------------------------- | ---------------------------- | ------ |
| POST    | `/api/v1/sessions/{sessionId}/exercises/{position}`     | Required | `SessionExerciseRequestDTO`       | `WorkoutSessionResponseDTO`  | 200 |
| PUT     | `/api/v1/sessions/{sessionId}/exercises/{position}`     | Required | `SessionExerciseRequestDTO`       | `WorkoutSessionResponseDTO`  | 200 |
| DELETE  | `/api/v1/sessions/{sessionId}/exercises/{position}`     | Required | None                              | None                         | 204 |


### Session Sets

| Método | Endpoint                                                                    | Auth     | Request                     | Response                    | Status |
| ------- | --------------------------------------------------------------------------- | -------- | --------------------------- | --------------------------- | ------ |
| POST    | `/api/v1/sessions/{sessionId}/exercises/{position}/sets/{setNumber}`        | Required | `SessionSetRequestDTO`      | `WorkoutSessionResponseDTO` | 200 |
| PUT     | `/api/v1/sessions/{sessionId}/exercises/{position}/sets/{setNumber}`        | Required | `SessionSetRequestDTO`      | `WorkoutSessionResponseDTO` | 200 |
| DELETE  | `/api/v1/sessions/{sessionId}/exercises/{position}/sets/{setNumber}`        | Required | None                        | None                        | 204 |

---

## Progress Records

| Método | Endpoint                         | Auth     | Request                    | Response                           | Status |
| ------- | -------------------------------- | -------- | -------------------------- | ---------------------------------- | ------ |
| POST    | `/api/v1/progress`              | Required | `ProgressRecordRequestDTO` | `ProgressRecordResponseDTO`        | 201 |
| GET     | `/api/v1/progress?from=&to=`              | Required | None                       | `Page<ProgressRecordResponseDTO>`  | 200 |
| DELETE  | `/api/v1/progress/{id}`         | Required | None                       | None                               | 204 |

---

## Statistics

### Session Volume

| Método | Endpoint                                      | Auth     | Request | Response                   | Status |
| ------- | --------------------------------------------- | -------- | ------- | -------------------------- | ------ |
| GET     | `/api/v1/stats/volume?sessionId={sessionId}` | Required | None    | `SessionVolumeResponseDTO` | 200 |

### Estimated 1RM

| Método | Endpoint                                  | Auth     | Request | Response               | Status |
| ------- | ----------------------------------------- | -------- | ------- | ---------------------- | ------ |
| GET     | `/api/v1/stats/1rm?exerciseId={id}`      | Required | None    | `OneRepMaxDTO`         | 200 |

### Active Streak

| Método | Endpoint                 | Auth     | Request | Response          | Status |
| ------- | ------------------------ | -------- | ------- | ----------------- | ------ |
| GET     | `/api/v1/stats/streak`  | Required | None    | `StreakDTO`       | 200 |

### Exercise Progression

| Método | Endpoint                                                        | Auth     | Request | Response                         | Status |
| ------- | --------------------------------------------------------------- | -------- | ------- | -------------------------------- | ------ |
| GET     | `/api/v1/stats/progress?exerciseId={id}&from={from}&to={to}`  | Required | None    | `ExerciseProgressResponseDTO`    | 200 |
