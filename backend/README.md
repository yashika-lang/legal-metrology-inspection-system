# Backend — Legal Metrology AI Inspection System

Spring Boot 3 / Java 21 REST API. This README covers everything needed to
get the backend running locally and connected to Supabase. For the overall
system design, see [`../docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md).

## What's implemented in this phase

This phase delivers the **backend foundation**, not the AI features or
dashboards (those are later phases — see "What's deferred" below).

- Full project setup: Maven, Java 21, layered/feature-sliced package
  structure under `com.legalmetrology`, Docker + docker-compose, Swagger/OpenAPI.
- Complete database schema (Flyway migrations) for every entity in the
  system, plus seed data (roles, a default admin account, and the Legal
  Metrology mandatory-declaration rule master data).
- Full authentication: signup, login, JWT access + rotating refresh tokens,
  logout, forgot/reset password, role-based authorization (`ADMIN`,
  `SENIOR_OFFICER`, `INSPECTOR`).
- Working CRUD + business rules for: Users (admin management), Products
  (with categories/manufacturers), Inspections (with a status-transition
  state machine and status history), Images (uploaded to Supabase Storage,
  with a perceptual hash computed for future duplicate detection), Scans
  (barcode/QR/manual, with basic product matching), Reports (read-only —
  see below), Audit log / history, Settings.
- Supabase Storage integration (`storage` module): upload/download/delete/
  signed-URL/temp-file operations, used by the image upload flow today and
  ready for reports/exports later.
- Cross-cutting foundation: global exception handling → standard
  `{success, message, data, timestamp, errors}` envelope on every response,
  custom Bean Validation annotations, JWT/DateTime/PDF/File/Image/Response
  utilities, request logging with a per-request trace id, Docker.
- Sample unit tests for the authentication service and JWT token provider.

## What's deferred to later phases

The `ocr`, `vision`, `rules` (evaluation logic), `analytics`, and `ai`
packages are intentionally structural placeholders only — no business logic
lives there yet, per this phase's scope. Their **data model already
exists** (`OCRResult`, `Violation`, `Rule`, `AIResponse`, `ChatHistory`
entities and tables), so implementing those modules later is additive, not
a migration. Report *generation* (rendering a PDF/Excel from an
inspection's results) is likewise deferred — the `report` module currently
only reads whatever report records exist. The `dashboard` package and any
dashboard/analytics endpoints are not implemented yet. No frontend pages
are part of this phase.

## Prerequisites

- Java 21 (`java -version`)
- Maven 3.9+ (`mvn -version`) — or just use the wrapper if one is added later
- Docker + Docker Compose (optional, for containerized local development)
- A Supabase project (or a local Postgres — see below) and, eventually, a
  Supabase Storage bucket set for image uploads

> **If you have multiple JDKs installed**, make sure Maven is actually
> running on JDK 21, not just that a `java 21` exists somewhere on your
> machine. `mvn -version` prints the JDK Maven resolved (its `Java version:`
> line) — if that's anything other than 21, Lombok's annotation processor
> can fail against newer javac internals with an opaque
> `ExceptionInInitializerError: ... TypeTag :: UNKNOWN` at compile time.
> Fix it by pointing `JAVA_HOME` at your JDK 21 install for this project,
> e.g. `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` on macOS (or the
> equivalent for your OS), then re-run `mvn -version` to confirm.

## Environment variables

Copy `.env.example` to `.env` and fill in real values:

```bash
cp .env.example .env
```

Never commit `.env` — it's git-ignored at the repo root. Key variables:

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` \| `dev` \| `prod` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Postgres connection (Supabase or local) |
| `JWT_SECRET` | HMAC signing key for access tokens — generate with `openssl rand -base64 64` |
| `JWT_ACCESS_TOKEN_EXPIRATION_MS`, `JWT_REFRESH_TOKEN_EXPIRATION_MS` | Token lifetimes |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origin(s) |
| `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` | Supabase project + Storage access |
| `SUPABASE_BUCKET_*` | Bucket names (create these in Supabase Storage — see below) |

Full list with defaults: [`.env.example`](.env.example).

## Running locally

### Option A — plain Maven, against a local Postgres

```bash
# 1. Start a local Postgres that matches the schema (or point DB_URL at Supabase directly)
docker compose up -d postgres

# 2. Run the app (reads .env via your shell, or export the vars another way)
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run
```

The API starts on `http://localhost:8080`. Swagger UI:
`http://localhost:8080/swagger-ui.html`.

### Option B — full Docker Compose (API + Postgres)

```bash
docker compose up --build
```

### Running tests

```bash
mvn test
```

## Connecting to Supabase

1. Create a Supabase project. From **Project Settings → Database**, copy
   the connection string (use the connection *pooler* string, "Transaction"
   mode, for `DB_URL`) and set `DB_USERNAME`/`DB_PASSWORD` accordingly.
2. From **Project Settings → API**, copy the **service role key** (never
   the anon/public key) into `SUPABASE_SERVICE_ROLE_KEY` — the backend uses
   it server-side only, to talk to Supabase Storage with elevated
   privileges. It must never reach the frontend.
3. In **Storage**, create five private buckets matching the names in
   `.env.example`: `images`, `reports`, `temp`, `exports`, `training-data`
   (or set the `SUPABASE_BUCKET_*` variables to whatever you name them).
4. Set `SPRING_PROFILES_ACTIVE=dev` (or `prod`) once you're off a purely
   local database.

## Running migrations

Migrations run automatically on application startup (`spring.flyway.enabled=true`).
To run them manually against a target database without starting the app:

```bash
mvn flyway:migrate \
  -Dflyway.url="$DB_URL" \
  -Dflyway.user="$DB_USERNAME" \
  -Dflyway.password="$DB_PASSWORD"
```

Migration files live in `src/main/resources/db/migration/`:

- `V1__init_schema.sql` — full schema for every entity in the system.
- `V2__seed_data.sql` — seeds the three roles, a default admin account
  (`admin@legalmetrology.gov.in` / `Admin@123` — **change this immediately**
  outside local development), and the Legal Metrology mandatory-declaration
  rule master data.

Add new migrations as `V{n}__description.sql`; never edit an already-applied
migration file — Flyway checksums them.

## API documentation

Once running: `http://localhost:8080/swagger-ui.html` (disabled in the
`prod` profile). The raw OpenAPI JSON is always at `/v3/api-docs`.

## Project structure

See [`../docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md) §3–4 for full
rationale. Quick map: `com.legalmetrology` has cross-cutting packages
(`config`, `security`, `exception`, `validation`, `utils`, `common`) plus
one feature-sliced package per domain (`auth`, `product`, `inspection`,
`scanner`, `report`, `history`, `storage`, and the reserved-for-later
`dashboard`/`ocr`/`vision`/`rules`/`analytics`/`ai`), each internally
organized as `controller/ service/ service/impl/ repository/ entity/ dto/
mapper/`.
