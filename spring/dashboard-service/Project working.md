# Project working — run StudyNest locally

Step-by-step guide for running `dashboard-service` on your own machine:
local PostgreSQL + Keycloak in Docker + Spring Boot from IntelliJ.

| What | Where | Login |
|---|---|---|
| PostgreSQL (your machine) | `localhost:5432`, database `study_nest` | `postgres` / `1234` |
| Keycloak (Docker) | http://localhost:8080 | `admin` / `admin` |
| Mailpit (Docker, fake inbox) | http://localhost:8025 | – |
| dashboard-service | http://localhost:8081 | Keycloak token with role `ADMIN` |
| Swagger UI | http://localhost:8081/swagger-ui.html | paste a token |

---

## 0. Install once

- **JDK 21** (IntelliJ → *File → Project Structure → SDK*, e.g. `ms-21`)
- **Docker Desktop**, running
- **PostgreSQL** with an empty database `study_nest`
  ```sql
  CREATE DATABASE study_nest;
  ```
- **IntelliJ IDEA**, project opened at the `spring` folder (see *Troubleshooting* if Gradle sync fails)

---

## 1. Start Docker (Keycloak + Mailpit)

From the project root (`D:\HRD Class\StudyNest`, where `docker-compose.yml` is):

```powershell
docker compose up -d
docker compose ps        # both containers should be "running"
```

Wait ~30 s, then open http://localhost:8080 and log in with `admin` / `admin`.

> Stop with `docker compose down`. Keycloak settings are kept in the `keycloak-data` volume.
> `docker compose down -v` wipes them and you must redo steps 2–9.

---

## 2. Create the realm `lms`

1. Top-left dropdown (says **Keycloak / master**) → **Create realm**
2. **Realm name:** `lms` → **Create**

Make sure **lms** is selected in the dropdown for every step below (not `master`).

---

## 3. Create the realm roles

**Realm roles → Create role**, three times:

| Role name |
|---|
| `ADMIN` |
| `INSTRUCTOR` |
| `STUDENT` |

Names must be exactly upper-case — the code looks them up by name (`Roles.java`, `KeycloakAdminClient.assignRealmRole`).

---

## 4. Create client `lms-web` (the app users log in with)

**Clients → Create client**

| Page | Field | Value |
|---|---|---|
| General settings | Client type | `OpenID Connect` |
| | Client ID | `lms-web` |
| Capability config | Client authentication | **Off** (public client) |
| | Standard flow | On |
| | Direct access grants | **On** (lets you get a token with username/password for testing) |
| Login settings | Valid redirect URIs | `http://localhost:*` |
| | Web origins | `+` |

→ **Save**

---

## 5. Create client `dashboard-service` (backend → Keycloak Admin API)

The service uses this client to create users, assign roles, read events and change the password policy.

**Clients → Create client**

| Page | Field | Value |
|---|---|---|
| General settings | Client ID | `dashboard-service` |
| Capability config | Client authentication | **On** |
| | Standard flow | Off |
| | Direct access grants | Off |
| | Service accounts roles | **On** |

→ **Save**, then:

1. **Credentials** tab → copy the **Client secret** (you need it in step 10).
2. **Service account roles** tab → **Assign role** → filter dropdown **Filter by clients** → search `realm-management` → tick:
   - `manage-users`
   - `view-users`
   - `view-events`
   - `manage-realm`

   → **Assign**

---

## 6. Turn on event saving (login audit log)

**Realm settings → Events** tab → **User events settings**

1. **Save events:** On
2. **Event types** → **Add saved types** → add `LOGIN`, `LOGIN_ERROR`, `LOGOUT`, `UPDATE_PASSWORD`, `RESET_PASSWORD`
3. **Save**

`GET /api/admin/audit-log/logins` reads these.

---

## 7. Turn on brute-force detection (account lock / unlock)

**Realm settings → Security defenses → Brute force detection**

- **Brute Force Mode:** `Lockout temporarily`
- **Max login failures:** `5`
- **Wait increment:** `15 minutes`

→ **Save**. `POST /api/admin/users/{id}/unlock` clears this lock.

---

## 8. Set up email (Mailpit)

So the "set your password" email for new users is delivered to the fake inbox.

**Realm settings → Email** tab

| Field | Value |
|---|---|
| From | `noreply@studynest.local` |
| Host | `mailpit` *(container name — Keycloak and Mailpit share the Docker network)* |
| Port | `1025` |
| Encryption / Authentication | all off |

→ **Save**. (**Test connection** only works after your Keycloak `admin` user has an email —
set it in the top-right user menu → *Manage account*, or skip the test.)

If email is not set up, creating users still works — the service just logs
`Could not send first-login email` and you can use *resend-invite* later.

---

## 9. Create the first admin user

There is no sign-up page, so the first admin is created by hand in Keycloak.

1. **Users → Add user**
   - Username: `admin@studynest.local`
   - Email: `admin@studynest.local`
   - Email verified: **On**
   - First name: `System`, Last name: `Admin`
   → **Create**
2. **Credentials** tab → **Set password** → `Admin@1234`, **Temporary: Off** → **Save**
3. **Role mapping** tab → **Assign role** → filter **Filter by realm roles** → tick `ADMIN` → **Assign**
4. **Details** tab → copy the user **ID** (a UUID) — needed in step 12.

---

## 10. Give the service its Keycloak secret

IntelliJ → **Run → Edit Configurations… → DashboardServiceApplication**
(create it if missing: *+ → Spring Boot*, main class `com.client.lms.dashboard.DashboardServiceApplication`,
module `dashboard-service.main`).

**Environment variables:**

```
KEYCLOAK_ADMIN_CLIENT_SECRET=<secret from step 5>
```

Everything else already has a working default for local dev:

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/study_nest` |
| `DB_USER` / `DB_PASSWORD` | `postgres` / `1234` (`application-dev.yml`) |
| `KEYCLOAK_URL` / `KEYCLOAK_REALM` | `http://localhost:8080` / `lms` |
| `KEYCLOAK_APP_CLIENT_ID` | `lms-web` |
| `KEYCLOAK_ADMIN_CLIENT_ID` | `dashboard-service` |
| `PORT` | `8081` |

Profile `dev` is active by default (Swagger on, SQL logging on).

---

## 11. Run dashboard-service

Click ▶ on `DashboardServiceApplication` in IntelliJ.

On the first start Flyway creates schema **`dashboard`** in `study_nest` and runs
`V1__create_users`, `V2__create_audit_log`, `V3__create_import_jobs`. Look for:

```
Creating schema "dashboard" ...
Successfully applied 3 migrations to schema "dashboard"
Tomcat started on port 8081
```

Check it is up: http://localhost:8081/actuator/health → `{"status":"UP"}`

> No `gradlew` script is in the repo yet, so run from IntelliJ. To add the wrapper
> (any machine with Gradle installed): `cd spring` → `gradle wrapper --gradle-version 8.10.2`.
> Then `.\gradlew :dashboard-service:bootRun` works from the terminal.

---

## 12. Link the admin to the `users` table

The admin from step 9 exists only in Keycloak. Add the matching row so they show up in user lists
(run in pgAdmin / DBeaver on `study_nest`, after step 11 created the tables):

```sql
INSERT INTO dashboard.users (id, keycloak_user_id, email, full_name, role, status, preferred_language)
VALUES (gen_random_uuid(),
        '<Keycloak user ID from step 9>',
        'admin@studynest.local',
        'System Admin',
        'ADMIN',
        'ACTIVE',
        'en');
```

Users you create through the API later get their row automatically.

---

## 13. Get an access token

PowerShell:

```powershell
$token = (Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/realms/lms/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type = "password"
    client_id  = "lms-web"
    username   = "admin@studynest.local"
    password   = "Admin@1234"
  }).access_token

$token   # print it
```

Postman: **POST** same URL, body `x-www-form-urlencoded` with the same 4 fields, copy `access_token`.

The token lasts 5 minutes by default (change in **Realm settings → Tokens → Access Token Lifespan**).

> Always use `localhost:8080` (not `127.0.0.1`) — the token's issuer must match
> `issuer-uri` in `application.yml`, otherwise every call returns **401**.

---

## 14. Call the API

```powershell
$h = @{ Authorization = "Bearer $token" }

# Dashboard counts (classroom / processing services are not built yet — those counts come back empty)
Invoke-RestMethod -Uri "http://localhost:8081/api/admin/dashboard/stats" -Headers $h

# List users
Invoke-RestMethod -Uri "http://localhost:8081/api/admin/users" -Headers $h

# Create an instructor
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/admin/users/instructors" -Headers $h `
  -ContentType "application/json" `
  -Body '{"email":"teacher1@studynest.local","fullName":"Teacher One","preferredLanguage":"en"}'

# Create a student
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/admin/users/students" -Headers $h `
  -ContentType "application/json" `
  -Body '{"email":"student1@studynest.local","fullName":"Student One","studentCode":"S001","preferredLanguage":"km"}'
```

Or in **Swagger UI** (http://localhost:8081/swagger-ui.html): **Authorize** → paste the token
(without `Bearer `) → *Try it out*.

---

## 15. Check the result

1. **Keycloak → Users**: `teacher1@…` exists with role `INSTRUCTOR` and required action *Update Password*.
2. **Mailpit** http://localhost:8025: the "Update your account" email is there. Click the link →
   set a password → the new user can now log in (step 13 with their email/password).
3. **Database**: `SELECT * FROM dashboard.users;` and `SELECT * FROM dashboard.audit_log;` show the new rows.
4. **Login audit**: `GET /api/admin/audit-log/logins` lists the logins from step 13.

---

## 16. Run the tests

IntelliJ: right-click `dashboard-service/src/test/java` → **Run 'All Tests'**
(or `.\gradlew :dashboard-service:test` once the wrapper exists). No Docker / database needed — they are unit tests.

---

## Daily routine (after the first setup)

1. Start Docker Desktop → `docker compose up -d`
2. Make sure local PostgreSQL is running
3. ▶ `DashboardServiceApplication` in IntelliJ
4. Get a token (step 13) and test

---

## Troubleshooting

| Error | Cause → fix |
|---|---|
| `plugin is already on the classpath with an unknown version` (Gradle sync) | A Spring plugin was declared with a `version` in `spring/build.gradle.kts`. Versions live only in `buildSrc/build.gradle.kts`. |
| `Argument for @NotNull parameter 'buildSrcModuleNode' … must not be null` (Gradle sync) | IntelliJ stale state. *File → Invalidate Caches → Restart*; if still failing close IntelliJ, delete `.idea`, open the `spring` folder directly. |
| `28P01 password authentication failed for user "lms"` | Wrong DB login. Check `application-dev.yml` (`postgres` / `1234`) or set `DB_USER` / `DB_PASSWORD`. |
| `Found non-empty schema(s) "public" but no schema history table` | Flyway pointed at a schema that already has tables. Dev config uses schema `dashboard` — make sure `application-dev.yml` has `spring.flyway.schemas: dashboard`. |
| `Schema-validation: missing table [...]` | Flyway did not run or ran in another schema. Check `hibernate.default_schema` and `flyway.schemas` are both `dashboard`. |
| `Connection refused localhost:8080` | Keycloak container not running → `docker compose up -d`, check `docker compose logs keycloak`. |
| **401** on every API call | Token expired (5 min), wrong realm, or token taken from `127.0.0.1` instead of `localhost`. Get a new one. |
| **403** on `/api/admin/**` | The user has no `ADMIN` realm role (step 9.3). Get a **new** token after assigning the role. |
| `KeycloakOperationException` / HTTP 401 from Keycloak when creating users | Wrong `KEYCLOAK_ADMIN_CLIENT_SECRET`, or client `dashboard-service` missing *Service accounts roles* (step 5). |
| HTTP 403 from Keycloak when creating users / reading events | Service account is missing `manage-users`, `view-events` or `manage-realm` (step 5.2). |
| `Could not send first-login email` in the log | Email not configured (step 8) or Mailpit not running. User is still created; use `POST /api/admin/users/{id}/resend-invite`. |
| Port 8080 already in use | Something else (Tomcat, another app) uses it. Stop it, or change the compose port to `"8180:8080"` and set `KEYCLOAK_URL=http://localhost:8180`. |
