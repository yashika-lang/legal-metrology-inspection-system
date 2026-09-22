# Legal Metrology AI Inspection System

Enterprise, AI-powered inspection platform for Legal Metrology officers
(Government of India problem statement) — responsive web app + installable
PWA for field use, Spring Boot backend, Supabase Postgres/Storage/Auth, and
a multi-provider AI pipeline (OCR, Vision AI, LLM, deterministic rule
engine, semantic search, analytics).

**Architecture is fully designed before any application code is written.**
Start here:

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — full architecture: system
  design, frontend/backend structure, database schema, storage layout, the
  12-stage AI pipeline, API structure, reusable components, naming
  conventions, environment variables, library choices and rationale, and
  scalability plan.
- [`database/schema.sql`](database/schema.sql) — complete normalized
  PostgreSQL DDL (run against Supabase, or wrap as a Flyway migration).

## Repository layout

```
legal-metrology-inspection-system/
  frontend/     React + Vite + TypeScript SPA/PWA        → frontend/README.md
  backend/      Spring Boot (Java 21) REST API           → backend/README.md
  database/     SQL schema / migrations
  docs/         Architecture documentation
  storage-design/  Supabase Storage bucket conventions (see ARCHITECTURE.md §6)
```

## Status

Architecture and folder scaffolding complete. No application code has been
written yet — next step is implementation, module by module, against this
design.
