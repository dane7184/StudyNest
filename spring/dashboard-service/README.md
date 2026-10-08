# dashboard-service — Feature 1: Dashboard (Admin)

Spring Boot 3.3 / Java 21 / Gradle (Kotlin DSL) / PostgreSQL / Keycloak.

Admin manages users, roles, classrooms, password policy, MFA, unlocks and the audit log.
There is **no public sign-up**: every account is created here (one by one, or students by Excel import).
The service **never stores passwords**. `users.keycloak_user_id` links each row to the Keycloak account.

## Endpoints (all need the `ADMIN` realm role)

| Area | Method and path |
|---|---|
| Stats | `GET /api/admin/dashboard/stats` |
| Users | `POST /api/admin/users/instructors`, `POST /api/admin/users/students`, `GET /api/admin/users`, `GET /api/admin/users/{id}`, `GET /api/admin/users/{id}/classrooms`, `PATCH /api/admin/users/{id}`, `POST .../disable`, `POST .../enable`, `DELETE /api/admin/users/{id}`, `POST .../resend-invite` |
| Excel import | `POST /api/admin/users/import` (multipart `file`), `GET /api/admin/users/import/{jobId}` |
| Roles | `PUT /api/admin/users/{id}/role` |
| Account security | `POST /api/admin/users/{id}/unlock`, `POST .../reset-mfa`, `POST .../force-password-reset` |
| Security policy | `GET /api/admin/security`, `PUT /api/admin/security/password-policy`, `PUT /api/admin/security/mfa-policy` |
| Classrooms | `GET /api/admin/classrooms`, `GET /api/admin/classrooms/{id}`, `POST /api/admin/classrooms/{id}/archive` |
| Audit log | `GET /api/admin/audit-log/actions`, `GET /api/admin/audit-log/logins` |

Swagger UI: `http://localhost:8081/swagger-ui.html` (dev profile only).

## Excel import layout

First sheet, header row, one student per row:

| A: Full name | B: Email | C: Student ID | D: Classroom code (optional) | E: Language km/en (optional) |
|---|---|---|---|---|

The whole file is validated first. Then rows are created in the background.
Each row ends as `CREATED`, `SKIPPED` (already exists) or `ERROR`. Poll the job id for the result.

## Run locally

```bash
# from backend/spring
./gradlew :dashboard-service:bootRun
```

Environment variables (defaults in `application.yml`):

| Variable | Meaning |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL |
| `KEYCLOAK_URL`, `KEYCLOAK_REALM` | Keycloak server and realm |
| `KEYCLOAK_APP_CLIENT_ID` | Client the web/mobile apps log in with |
| `KEYCLOAK_ADMIN_CLIENT_ID`, `KEYCLOAK_ADMIN_CLIENT_SECRET` | Service-account client with `manage-users`, `view-events`, `manage-realm` roles |
| `CLASSROOM_SERVICE_URL`, `PROCESSING_SERVICE_URL` | Other services the dashboard reads counts from |

Keycloak needs: realm roles `ADMIN`, `INSTRUCTOR`, `STUDENT`; event saving on for
`LOGIN`, `LOGIN_ERROR`, `LOGOUT`, `UPDATE_PASSWORD`, `RESET_PASSWORD`; SMTP set up so the
first-login and password-reset emails are sent.

## Contracts with other services (to implement there)

classroom-service:
- `GET /internal/admin/stats` → `{ classrooms: {total, active, archived, courses}, files: {total, totalSizeBytes, byType} }`
- `GET /internal/admin/classrooms?q&status&page&size` → page of classroom summaries
- `GET /internal/admin/classrooms/{id}`, `POST /internal/admin/classrooms/{id}/archive`
- `POST /internal/admin/classrooms/enrol` `{userId, classroomCode}`
- `GET /internal/admin/users/{id}/classrooms`

content-processing-service (Python):
- `GET /internal/admin/jobs/stats` → `{ "QUEUED": n, "RUNNING": n, "DONE": n, "FAILED": n }`

## Tests

```bash
./gradlew :dashboard-service:test
```

Unit tests cover user creation rules, Excel parsing and the password-policy translation.
The controller test checks the ADMIN role guard and validation. Testcontainers (Postgres + Keycloak)
dependencies are included for integration tests to add later.

## Assumptions (not in Project Detail)

1. **Delete user = soft delete.** Keycloak account disabled, `users.status = DELETED`, data kept. Open decision.
2. Admin may not disable, delete or demote their own account.
3. Excel columns as above; max 2000 rows; max file 5 MB.
4. Dashboard shows counts only (no charts over time yet).
5. MFA policy per role is stored as realm attributes `lms.mfa.enabled` / `lms.mfa.roles`.
6. Feature 3 (Profile) is a separate service. This service only handles admin actions.
7. Password-policy defaults in the UI form: min length 8, 1 upper, 1 lower, 1 digit, lock after 5 failures for 15 minutes.
