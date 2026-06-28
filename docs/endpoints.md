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
