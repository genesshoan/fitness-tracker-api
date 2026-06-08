# Endpoints REST

## Base
Controllers expose with prefix **`/api/v1`**.

## Roles
'ROLE_ADMIN', 'ROLE_USER'.

## Auth
| Método | Endpoint | Roles | Request DTO | Response DTO |
|--------|----------|-------|-------------|--------------|
| POST | `/api/v1/auth/register` | PUBLIC | `RegisterRequestDTO` | `TokenResponseDTO` |
| POST | `/api/v1/auth/login` | PUBLIC | `LoginRequestDTO` | `TokenResponseDTO` |
| POST | `/api/v1/auth/refresh` | PUBLIC | - | `TokenResponseDTO` |
| DELETE | `/api/v1/auth/logout` | PUBLIC | - | `204 No Content` |
