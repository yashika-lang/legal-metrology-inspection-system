# Nirikshan AI — Complete Project Knowledge Document

**Purpose of this document**: raw, fact-checked material extracted directly from the actual codebase, database schema, configuration, and documentation of this project — for building the Smart India Hackathon presentation. Every claim below is traceable to a real file. Where something is aspirational, planned, or not yet built, it is labeled as such explicitly — nothing here is invented to sound more impressive than the current state of the system.

Two names are used inside the project and both are real, distinct things:
- **"Nirikshan AI"** — the platform/product name (frontend title, login screen, sidebar branding).
- **"Nirikshak"** — the in-app AI Copilot chat persona specifically (the drawer inside the Inspection Workspace). It is a feature name, not the product name.

---

## SECTION 1 — PROJECT OVERVIEW

### Project Name
**Nirikshan AI** (formally: *Legal Metrology AI Inspection System*; Maven artifact: `inspection-system`; backend module name: `legal-metrology-inspection-system`; backend `pom.xml` description: *"AI-powered Legal Metrology Inspection System — backend API"*).

### One-line Description (from README.md, verbatim)
> "Enterprise, AI-powered inspection platform for Legal Metrology officers (Government of India problem statement) — responsive web app + installable PWA for field use, Spring Boot backend, Supabase Postgres/Storage/Auth, and a multi-provider AI pipeline (OCR, Vision AI, LLM, deterministic rule engine, semantic search, analytics)."

*(Note: "installable PWA" and "semantic search" are stated aspirations in this description — PWA packaging has not been implemented and semantic search infrastructure exists in the database but is not wired to any feature. Both are flagged again in the relevant sections below.)*

### Tagline (5 options — drafted for this document; no tagline currently exists in the repo)
1. "Every inspection, explained." *(built around the actual Decision Trace / Explainable Evidence Framework)*
2. "AI reads the label. The Rule Engine decides. You stay in control." *(built around the real architecture: AI never overrides compliance decisions)*
3. "From photo to legal evidence, in one pipeline."
4. "Compliance you can audit, not just trust."
5. "Nirikshan AI — inspection intelligence for Legal Metrology."

### Elevator Pitch (30 seconds — synthesized from real, implemented capabilities)
Nirikshan AI is a government-grade inspection platform that lets a Legal Metrology officer photograph a product label and get a fully explainable compliance verdict in seconds. Behind the scenes, Google Vision OCR and Gemini/Claude Vision AI independently read the label, a fusion engine reconciles their findings, and a fully database-driven Rule Engine — never the AI — decides whether each of 19 active legal rules (MRP declaration, net quantity, manufacturer address, FSSAI license, and more) is satisfied. Every decision is backed by cropped, annotated photographic evidence with a SHA-256 integrity hash, and every pipeline step is recorded in an append-only Decision Trace, so the resulting compliance score and violation list are legally defensible, not a black box. An AI Copilot ("Nirikshak") can explain any rule or violation in plain language — but it is architecturally incapable of deciding compliance itself, since it only ever sees facts the Rule Engine already produced.

### Vision (drafted — no verbatim vision statement exists anywhere in the repo)
A Legal Metrology inspection system where every compliance decision made anywhere in India is fast, consistent regardless of which officer makes it, and fully explainable — with photographic evidence and an auditable decision trail — to the manufacturer, the officer's superior, and a court of law alike.

### Mission (drafted — no verbatim mission statement exists in the repo)
Replace manual, subjective, undocumented label inspection with an AI-assisted pipeline where AI does the reading and explaining, while a transparent, versioned, database-driven Rule Engine — not a model — makes every compliance determination.

### Problem Statement Summary
The repo's own framing (README.md) labels this explicitly as a **"Government of India problem statement"**, without further elaboration — no detailed narrative problem statement, and no cited statistics (inspector counts, product volumes, etc.) exist anywhere in the repository's documentation. `docs/ARCHITECTURE.md` gives the only quantified scale assumption found anywhere in the docs: the system is architected for **"government-inspection volumes (thousands, not millions, of inspections/day)"** — a design assumption, not a cited real-world statistic. Based strictly on what the system is built to solve (inferred from its actual feature set, not asserted as external fact): manual inspection of packaged-commodity labels against the Legal Metrology (Packaged Commodities) Rules is slow, inconsistent between officers, produces no reusable photographic evidence, and leaves no auditable record of why a product was judged compliant or non-compliant.

### Objectives (derived from what was actually built, phase by phase per CHANGELOG.md)
1. Digitize label inspection: photo in, structured compliance data out.
2. Combine OCR and Vision AI so no single provider's blind spot silently passes a violation.
3. Keep compliance determination 100% deterministic and rule-based, never delegated to an LLM.
4. Generate durable, cryptographically-hashed photographic evidence for every violation.
5. Record a complete, replayable audit trail of the entire decision pipeline per inspection.
6. Give officers a natural-language assistant that explains and recommends, without being able to alter or second-guess a compliance verdict.
7. Surface trend, risk, and anomaly analytics across manufacturers, categories, regions, and inspectors using only already-decided, persisted data.
8. Produce a court/manufacturer-ready Smart Report (PDF + DOCX) combining all of the above.

### Why This Project Is Needed
Grounded in the real architecture (not external statistics, since none are cited in the repo): a compliance system that relies on AI judgment alone is not legally defensible and is vulnerable to hallucination on the exact numeric facts (a compliance score, a violation count) that must have exactly one correct answer. This project's actual design answer — confirmed directly in code and CHANGELOG.md — is to let AI do what it's good at (reading noisy, multi-language label photos; explaining a rule in plain language) while a versioned, auditable, deterministic Rule Engine does what a legal decision requires: complete, "boring," reproducible correctness.

---

## SECTION 2 — UNIQUE VALUE PROPOSITION

**What makes this different from "a normal OCR system":** a normal OCR system stops at text extraction. This system treats OCR as only one of two independent evidence sources (the other being Vision AI's own label understanding), fuses them, evaluates the fused result against a versioned legal rule set, generates cryptographically-hashed photographic evidence for every failure, and records a full audit trail — with the AI explicitly walled off from ever making the actual compliance call. Every feature below is real and implemented unless marked otherwise.

| Feature | Exists? | Why it exists | How it works | Why it's useful |
|---|---|---|---|---|
| **AI Vision (declaration detection)** | ✅ Built | OCR alone can miss where on the package a declaration is or whether it's on the correct panel | `GeminiVisionProvider`/`ClaudeVisionProvider` send the image with a shared prompt asking the model to detect every `DeclarationType` (MRP, net quantity, mfg date, etc.), its value, confidence, bounding box, and which package face it's on | Confirms *placement* and *presence*, not just legible text — catches violations OCR alone would miss (e.g. MRP on the wrong panel) |
| **OCR + AI Correction** | ✅ Built | Raw OCR often confuses visually similar characters (I/1, O/0, S/5) | `OcrCorrectionServiceImpl` sends raw OCR text to the configured LLM with a strict prompt ("fix only clear character-confusion errors, never invent or translate"), expects structured JSON back, and falls back to raw text at confidence 0.0 if parsing fails | Recovers legible values from noisy real-world photos without letting the LLM silently rewrite content |
| **OCR Normalization** | ✅ Built (separate from AI correction — pure rule-based, no AI) | Currency, units, dates, and phone numbers appear in dozens of inconsistent raw forms | Regex/lookup-table cleanup: currency → `₹`, unit aliases (`gm`/`gms`→`g`), month-year parsing, `+91` phone prefixing, email/PIN regex extraction | Deterministic, zero hallucination risk for formatting — reserved for cases with exactly one correct normalized form |
| **Rule Engine** | ✅ Built | Compliance decisions must be legally defensible, versioned, and never dependent on a model's mood | Fully database-driven: each of 19 active rules is a `rules` row with one of 12 reusable validator strategies (`FIELD_EXISTS`, `REGEX`, `NUMERIC`, `FONT_SIZE`, `READABILITY`, `PLACEMENT`, `CUSTOM`, etc.) plus a JSON expression; rules are versioned (never mutated, only superseded); cached in memory at startup | New/changed rules ship as data, not code deploys; every decision is 100% reproducible from a specific rule version |
| **Explainable AI (boundary enforcement)** | ✅ Built | An LLM must never be able to decide or override a legal compliance outcome | `CopilotContext` is a read-only snapshot assembled *after* the Rule Engine/Scoring service already ran; the Copilot's own system prompt states: *"You NEVER decide, determine, override, or second-guess compliance status... Treat every fact in the provided context as ground truth."* Purely factual questions bypass the LLM entirely, answered deterministically from the database | Removes hallucination risk for the numbers that matter (score, violation count) while still getting natural-language explanation |
| **Evidence Engine** | ✅ Built | A violation needs durable proof, not just a database row | For each violation, walks back to the winning OCR/Vision detection, crops and annotates the source image region (color-coded by severity: red=CRITICAL, orange=MAJOR, yellow=MINOR) using plain Java AWT, computes a SHA-256 hash over every legally-relevant field, and becomes immutable once the inspection is marked completed | Produces a tamper-evident, per-violation photographic record suitable for a manufacturer dispute or legal proceeding |
| **Decision Trace** | ✅ Built | A compliance decision must be replayable and auditable after the fact | Append-only `decision_trace_steps` rows, one per pipeline stage (`IMAGE_UPLOADED` → `OCR` → `VISION_DETECTION` → `DECLARATION_FUSION` → `RULE_EVALUATION` → `COMPLIANCE_SCORE` → `EVIDENCE_GENERATED` → `REPORT_GENERATED`), each with confidence, timing, and status | Complete, ordered timeline of exactly what happened, in what order, how confidently — the backbone of "explainable AI" here |
| **Analytics** | ✅ Built | Individual inspections need to become organizational insight | 13 aggregate endpoints (KPIs, timeseries, heatmaps, treemaps, leaderboards, severity distribution, drill-down) computing only from already-persisted inspection/violation records | Lets supervisors see patterns (repeat offenders, worst categories/regions) without re-deriving anything from raw AI output |
| **Risk Index** | ✅ Built | Some manufacturers/regions/inspectors are systematically riskier and deserve prioritized attention | Weighted composite formula: `100 × (0.4·violationRateNormalized + 0.3·criticalRatio + 0.3·complianceGap)`, computed per manufacturer/category/region/inspector, with bands (LOW/MEDIUM/HIGH ≥40/≥70) and a full `Explainability` record (methodology, reasoning, confidence, assumptions) | A transparent, formula-based prioritization score — not an opaque ML risk model |
| **Compliance Score** | ✅ Built | A single, defensible per-inspection number is needed alongside the detailed violation list | `100 + Σ severityWeight(violation)` (CRITICAL −25, MAJOR −10, MINOR −3) minus small penalties for poor image quality, low OCR confidence, poor readability, and missing mandatory fields — weights configurable at runtime via `settings` table | One number for a dashboard, backed by a fully inspectable formula, not a model's opinion |
| **AI Copilot ("Nirikshak")** | ✅ Built | Officers need plain-language help without compromising decision integrity | 6 real capabilities: `ask` (general Q&A, deterministic-first), `explainRule`, `summarizeInspection`, `generateOfficerNotes`, `generateManufacturerRecommendations`, `compareInspections` — each hits its own dedicated backend endpoint, not a generic chat wrapper | Genuinely useful assistant behavior with an architecturally enforced compliance boundary |
| **Smart Reports** | ✅ Built | Officers and manufacturers need a finished, professional document, not raw JSON | PDF (OpenPDF) and DOCX (Apache POI) generation; only the executive summary and recommendations are AI-narrated (via the Copilot), with a deterministic fallback ("Inspection X recorded a compliance score of Y with Z violations...") if the LLM is unavailable — "the compliance facts in a report must never depend on an LLM being up" | Report generation never breaks just because an external AI API is down |
| **Officer Portal** | ⚠️ Partially built | The primary user of this system is the field inspector | Only the AI Inspection Workspace (3-panel: capture/upload, annotated image viewer, results + decision timeline) plus login and a "new inspection" redirect are real; Dashboard, Reports, Analytics, standalone AI Copilot page, Rules, Settings are placeholder screens | This is the actual, functioning demo surface for SIH — be precise about this scope in the pitch |
| **Consumer Portal** | ❌ Not built | Was discussed conceptually in an earlier design session but explicitly deferred ("STOP" instruction) and never started | Zero code exists | Do not claim this exists — present as future scope only |
| **Dashboard** | ❌ Not built (placeholder only) | Route exists (`/`) but renders a generic "Coming Soon" component with zero feature-specific code | — | Same — future scope, not current capability |
| **Image Quality Analysis** | ✅ Built | Poor photos should be flagged before they poison a compliance decision | Pure-Java heuristics (no OpenCV/ML): Laplacian-variance blur, brightness, contrast (std-dev), Sobel-based glare/rotation detection, resolution check, crop detection — produces a 0-100 score and one of 9 warning types (`BLUR`, `GLARE`, `LOW_RESOLUTION`, etc.) with a recommended action (`RETAKE`/`ENHANCE`/`NONE`) | Prevents a blurry or glare-heavy photo from silently causing a false violation |
| **Font Analysis** | ✅ Built | Legal Metrology rules require certain declarations to be a minimum, readable size | Per detected declaration's bounding box: font-size estimate (box-height % of image height — an explicit relative proxy, not a certified mm measurement), readability score (contrast + edge-density blend), contrast score; classifies `UNREADABLE`/`TOO_SMALL`/`HIDDEN`/`NONE` | Feeds two real rules (`LM-FONTSIZE-001`, `LM-NETQTY-FONT-001`) — catches "technically present but illegible" violations |
| **Label Completeness Detection** | ⚠️ Not a separate module | It's a byproduct of declaration fusion, not an independent engine | The Copilot's deterministic answer service filters fused declarations for `!present()` when asked "what's missing" | Real capability, but should not be pitched as a dedicated AI module — it's simple filtering over already-fused data |
| **Semantic Search** | ❌ Not implemented | `pgvector` extension and an `embeddings` table exist (migration V3), and Gemini's embedding model is configured in `application.yml` | No `ai.embedding` package or service exists anywhere in the source tree — confirmed by exhaustive search | Wired-for-later infrastructure only; do **not** claim this as a working feature |
| **Prediction Engine** | ✅ Built, but NOT AI/LLM-based | Trend forecasting and anomaly flagging are useful, but must never hallucinate a number | Ordinary least-squares linear regression (`LinearRegression.java`) for next-month violation/workload forecasts with R² and confidence intervals; z-score-based statistical anomaly detection | Real, working feature — but be precise in the pitch that this is classical statistics, not machine learning, by explicit design choice |

---

## SECTION 3 — COMPLETE TECH STACK

*Only technologies actually found in the codebase are listed. Nothing here is a typical/assumed choice.*

### Frontend (`frontend/package.json`, 41 total npm dependencies)
| Role | Technology | Version |
|---|---|---|
| Framework | React | ^19.0.0 (+ react-dom ^19.0.0) |
| Language | TypeScript | ^5.7.2 |
| Build tool | Vite | ^6.0.7 (+ @vitejs/plugin-react ^4.3.4) |
| Styling | Tailwind CSS | ^4.0.0 (`@theme`-block token system), + clsx, tailwind-merge |
| Component library pattern | Radix UI primitives + class-variance-authority | hand-authored shadcn/ui-style (Slot, Dialog, Tabs, Tooltip, Separator, ScrollArea, Progress, Avatar, DropdownMenu, Label, Select) |
| Charts | Recharts | ^2.15.0 |
| State management (server) | TanStack Query | ^5.62.11 |
| HTTP client | Axios | ^1.7.9 |
| Routing | React Router | ^7.1.1 (data router) |
| Forms/validation | React Hook Form + Zod | ^7.54.2 / ^3.24.1 |
| Animation | Framer Motion | ^11.15.0 |
| Icons | Lucide React | ^0.469.0 |
| Camera | none (raw `navigator.mediaDevices.getUserMedia`, no library) | — |
| Barcode | @zxing/browser + @zxing/library | ^0.1.5 / ^0.21.3 |
| File upload | react-dropzone | ^14.3.5 |
| PDF viewer | **Not present** — no PDF-viewing dependency found in the frontend | — |
| Fonts (self-hosted) | @fontsource-variable/inter, @fontsource/jetbrains-mono | ^5.1.1 |
| Responsive strategy | Standard Tailwind responsive utilities only — no dedicated mobile breakpoint strategy has been designed; the one built screen is desktop-oriented | — |
| Global state library | **None** — no Redux/Zustand found; state is TanStack Query (server) + two React Contexts (Auth, Theme) + local `useState` | — |

### Backend
| Role | Technology |
|---|---|
| Framework | Spring Boot 3.3.4 |
| Language | Java 21 (with virtual threads enabled: `spring.threads.virtual.enabled: true`) |
| Authentication | Custom JWT (own `users`/`refresh_tokens`/`password_reset_tokens` tables) — **not** Supabase Auth, despite an outdated `backend/README.md` claiming otherwise; this was a deliberate Phase 1 architectural decision |
| Security | Spring Security, role-based authorization, bearer-JWT scheme, CSP headers (added Phase 4) |
| APIs | 87 REST endpoints across 18 `@RestController` classes |
| API documentation | springdoc-openapi 2.6.0 (`/v3/api-docs`, `/swagger-ui.html`) |
| Logging | Logback (`logback-spring.xml`), console appender (local profile) / console+rolling-file+error-file appenders (dev/prod profiles) |
| Validation | Jakarta Bean Validation (Hibernate Validator 8.0.1) |
| Background processing | Java 21 virtual threads for concurrent blocking I/O (no message broker/queue found in the dependency set) |
| Object mapping | MapStruct (DTO↔entity mapping) |
| Report generation | OpenPDF (PDF), Apache POI (DOCX) |
| OCR native fallback | Tess4J / JNA (`libtesseract`) |

### Database
| Component | Detail |
|---|---|
| PostgreSQL | Primary datastore, 26 tables, accessed via Spring Data JPA/Hibernate |
| Flyway | 12 migrations (`V1`–`V12`), schema owned entirely by Flyway (`ddl-auto: validate`, never `update`) |
| pgvector | Extension enabled + `embeddings` table (V3) — provisioned, **not yet consumed** by any feature |
| Supabase Storage | 3 real buckets in production use: `inspection-images`, `inspection-reports`, `evidence` (3 more — `temp`, `exports`, `training-data` — are reserved/unused) |

### AI
| Component | Detail |
|---|---|
| Google Vision | OCR provider (`DOCUMENT_TEXT_DETECTION`), service-account OAuth2 or API-key auth |
| Gemini | Default provider for both Vision AI and LLM/Copilot; model `gemini-3.6-flash` for both (switched from deprecated `gemini-2.5-pro` after live verification found it 404s and free-tier "pro" models have zero quota) |
| Claude | Configurable alternate provider for both Vision AI and LLM; model `claude-opus-4-1` |
| OCR providers | Google Vision (primary) + Tesseract (offline fallback, multi-script: eng+hin+mar+tam+guj) |
| Vision providers | Gemini Vision, Claude Vision (declaration detection + package-face classification) |
| LLM providers | Gemini, Claude (Copilot: ask/explain/summarize/notes/recommend/compare) |
| Embedding model (configured, unused) | `text-embedding-004`, 768 dimensions |

### Infrastructure
| Component | Detail |
|---|---|
| Docker | Local Postgres dev container (port 55432, to avoid conflicting with a native Postgres on 5432) |
| Maven | Backend build (`mvn clean package`) |
| Git | Version control |
| Cloudflare Tunnel | Used to expose the local dev frontend for remote review (`cloudflared tunnel --url`, ephemeral `*.trycloudflare.com` hostname) |
| Environment configuration | `.env` files (backend + frontend, never committed), `application.yml` reading every value via `${VAR:default}` substitution, `ServiceNotConfiguredException` (503) for graceful degradation when an AI/storage credential is missing rather than crashing the app |

---

## SECTION 4 — SYSTEM ARCHITECTURE

### High-Level Architecture
A **modular monolith** — a single Spring Boot deployable, deliberately not microservices. `docs/ARCHITECTURE.md`'s own justification: *"the AI pipeline is a single sequential workflow... Splitting it into services now would mean distributed transactions and network hops for zero scaling benefit at government-inspection volumes (thousands, not millions, of inspections/day)."* React SPA frontend ↔ Spring Boot REST API ↔ PostgreSQL (Supabase-hosted) + Supabase Storage ↔ external AI APIs (Google Vision, Gemini, Claude).

### Module Architecture (21 top-level backend packages under `com.legalmetrology`)
`ai, analytics, auth, common, config, dashboard, evidence, exception, history, inspection, ocr, product, report, rules, scanner, security, storage, trace, utils, validation, vision` — feature-sliced (each package owns its own controller/service/repository/entity/dto), not layered-by-type.

### Data Flow (per inspection)
1. Officer creates an `Inspection`, uploads label photo(s) → `Image` records + Supabase Storage upload.
2. `ImageQualityAnalyzer` scores each photo; poor photos flagged for retake.
3. OCR (`GoogleVisionOcrProvider`/`TesseractOcrProvider`) and Vision AI (`GeminiVisionProvider`/`ClaudeVisionProvider`) run, each producing independent `LabelDetection` records.
4. `DeclarationFusionService` reconciles OCR + Vision AI signals per declaration type into `FusedDeclaration` rows (confidence-weighted).
5. `RuleEvaluationService` evaluates all 19 active rules against the fused declarations → `Violation` rows + `ComplianceScoreServiceImpl` computes the score.
6. `EvidenceServiceImpl` generates annotated/original evidence images + SHA-256 hash per violation.
7. Every step above writes a `DecisionTraceStep` row.
8. `ReportDataAssemblerImpl` later assembles a full `ReportData` (deterministic facts + 2 AI-narrated fields) into PDF/DOCX.
9. `AnalyticsQueryService`/`InsightController` aggregate across many completed inspections for dashboards, forecasts, and risk indices — reading only persisted facts, never raw AI output.

### AI Flow
Two independent "senses" (OCR text extraction vs. Vision AI declaration/placement detection) run in parallel and are fused before anything touches the Rule Engine. The Rule Engine's output (violations + score) is the *only* thing the AI Copilot is later allowed to see (`CopilotContext`) — it can explain that output but architecturally cannot regenerate or override it.

### Backend Flow
Controller → Service (interface + `*ServiceImpl`) → Spring Data JPA Repository → Entity, per feature package; MapStruct mappers convert between entities and DTOs at the controller boundary; global exception handling via `@RestControllerAdvice`.

### Frontend Flow
Vite dev server → React Router (data router, 11 routes) → page component → TanStack Query hook (e.g. `useInspection`, `usePipeline`) → `apiClient` (axios, JWT bearer + single-flight refresh interceptor) → backend REST API → `ApiResponse<T>` envelope unwrapped by a shared `unwrap()` helper.

---

## SECTION 5 — FRONTEND

**Be precise about scope**: only the Officer Portal exists, and within it, only 3 of 11 routes are real.

| Screen | Status | Detail |
|---|---|---|
| Login | ✅ Real | Functional email/password form, calls `authApi.login`, stores JWT. Explicitly commented in code as *not* a designed marketing login screen — built only to reach the workspace. |
| New Inspection | ✅ Real (thin) | Fires `POST /inspections` on mount and redirects — not a designed intake form yet. |
| **AI Inspection Workspace** | ✅ **Real — the primary deliverable** | 3-panel layout: left = capture/upload panel (drag-drop via react-dropzone, camera capture via raw `getUserMedia`, barcode scan via ZXing); center = zoomable annotated image viewer with bounding-box overlays; right = violations/results panel; bottom = Decision Timeline. Header has "Run AI Pipeline" and the "Nirikshak" AI Copilot trigger (opens a slide-over chat drawer). |
| Dashboard, Products, Reports, Analytics, History, Rules, Settings, standalone AI Copilot page | ❌ Placeholder only | All render a generic `ComingSoonPage`/`EmptyState` component with zero feature-specific code. The AI Copilot itself is fully functional but only reachable embedded inside the Workspace drawer — the standalone `/copilot` route is not wired to it. |
| Consumer Portal | ❌ Not started | Discussed conceptually, explicitly deferred, zero code exists. |
| Admin Dashboard | ❌ Not started | Backend has admin-gated endpoints (user role/status management, rule CRUD); no dedicated admin UI exists. |

**Authentication**: `AuthContext` (React Context) manages user/login/logout/token bootstrap; JWT stored via `tokenStore`; axios interceptor attaches `Authorization: Bearer` and performs single-flight refresh on 401.

**Theme**: Light/dark via a `.dark` class on `<html>`, driven by `ThemeContext` (localStorage key `nirikshan.theme`, defaults to OS `prefers-color-scheme`). Every color is a CSS custom property overridden per theme, not a separate token set.

**Design System** (`frontend/src/styles/globals.css`, Tailwind v4 `@theme` block — explicit reference points cited in the file's own comment: "Linear / Vercel Dashboard / Stripe Dashboard"):
- **Typography**: Inter Variable (`--font-sans`, self-hosted via `@fontsource-variable/inter`) for UI text, JetBrains Mono (`--font-mono`) for data/code.
- **Color palette (light)**: background `#ffffff`, surface `#fafafa`, foreground `#0a0a0b`, border `#e4e4e7`, accent (indigo) `#4f46e5`, plus semantic success `#16a34a` / warning `#d97706` / critical `#dc2626` / info `#0284c7` (each with `-soft`/`-foreground` variants).
- **Color palette (dark)**: background `#09090b`, surface `#0f0f12`, foreground `#f7f7f8`, accent `#818cf8`, success `#4ade80`, warning `#fbbf24`, critical `#f87171`, info `#38bdf8`.
- **Radius**: 0.375rem / 0.5rem / 0.75rem / 1rem (sm/md/lg/xl). **Shadows**: layered `xs`–`lg` + a `glow` variant. **Animations**: fade-in, slide-up, scale-in, pulse-ring.
- No dedicated spacing/type-scale tokens beyond Tailwind's defaults.

**Icons**: Lucide React exclusively.

**Components** (`frontend/src/components/ui/`, 13 files): avatar, badge, button, card, dropdown-menu, input, progress, scroll-area, separator, sheet, skeleton, tabs, tooltip — hand-authored shadcn/ui pattern (Radix primitive + `class-variance-authority` variants + a local `cn()` helper), not CLI-scaffolded.

**State management**: server state via TanStack Query exclusively (`staleTime: 30s`, `retry: 1`); UI state via local `useState`/`useRef`; cross-cutting state via two Contexts (`AuthContext`, `ThemeContext`). No Redux/Zustand.

**API Integration**: one shared axios instance (`apiClient.ts`), base URL from `VITE_API_BASE_URL`; each Copilot capability calls its own dedicated backend endpoint rather than a single generic chat call — e.g. `ask`, `explainRule`, `summarize`, `generateOfficerNotes`, `manufacturerRecommendations`, `compareInspections` are six distinct API functions.

**Notifications / Profile settings pages**: not implemented as dedicated screens.

---

## SECTION 6 — BACKEND (module by module)

- **Auth** (`auth`): signup/login/refresh/logout/forgot-password/reset-password/me (`AuthController`), plus admin user management (`UserController`: list/get/update profile/roles/status). Custom JWT, not Supabase Auth.
- **OCR** (`ocr`): provider abstraction (Google Vision / Tesseract) with Throwable-safe fallback, normalization (rule-based), correction (LLM-based), declaration fusion, run history.
- **Vision** (`vision`): Gemini/Claude declaration detection, image quality analysis, font/readability analysis.
- **Rules** (`rules`): fully DB-driven rule definitions + 12 validator strategies, versioning, in-memory cache, evaluation engine, compliance scoring.
- **Evidence** (`evidence`): annotated/cropped image generation, SHA-256 hashing, immutability-on-completion.
- **Analytics** (`analytics`): 13 aggregate endpoints + an "Intelligence Layer" (`InsightController`: insights, forecasts, anomalies, risk index) — 7 more endpoints.
- **AI** (`ai`): LLM provider abstraction, Copilot (context assembly, prompt templates, 6 capabilities, deterministic-first answering for factual questions).
- **Reports** (`report`): report data assembly (deterministic + 2 AI-narrated fields with fallback), PDF (OpenPDF) and DOCX (Apache POI) rendering.
- **Decision Trace** (`trace`): append-only per-stage pipeline audit log.
- **Repository layer**: Spring Data JPA repositories per feature package (one per aggregate root).
- **Storage** (`storage`): Supabase Storage abstraction (`StorageServiceImpl`), `StorageBucket` enum (3 active buckets, 3 reserved), graceful `ServiceNotConfiguredException` on missing credentials.
- **Controllers**: 18 `@RestController` classes, 87 total endpoints.
- **Security** (`security`): JWT filter chain, Spring Security role-based authorization, bearer-auth Swagger scheme, CSP headers.
- Other supporting packages: `product` (products/manufacturers/categories/barcode lookup), `scanner` (barcode/QR/manual scan logging), `inspection` (lifecycle + image upload), `history` (audit log), `common`/`config`/`exception`/`utils`/`validation`/`dashboard`.

---

## SECTION 7 — AI MODULES (purpose / input / output / confidence / limitations / models / fallback)

| Module | Purpose | Input | Output | Confidence | Limitations | Models | Fallback |
|---|---|---|---|---|---|---|---|
| OCR (Google Vision) | Extract label text with structure | Image bytes | Paragraph→line→word tree, 0–1 relative bounding boxes, per-word confidence | Per-word confidence from Vision API | English/Hindi/Marathi/Tamil/Gujarati hints only | Google Cloud Vision `DOCUMENT_TEXT_DETECTION` | Falls back to Tesseract on any `Throwable` |
| OCR (Tesseract) | Offline OCR when Vision fails/unconfigured | Image bytes | Same paragraph/line/word structure, geometrically reconstructed | Tesseract's own word confidence | Requires local native lib + trained data; lower accuracy than Vision | Tess4J/JNA, `eng+hin+mar+tam+guj` | None — last resort; failure surfaces as "All OCR providers failed" |
| OCR Correction | Fix character-confusion OCR errors | Raw OCR text | Corrected text + confidence + change summary | Model-reported, 0.0 if parse fails | Explicitly forbidden from inventing/translating content | Gemini or Claude (configurable) | Falls back to raw uncorrected text at confidence 0.0 |
| Vision AI (declaration detection) | Detect presence/value/location/panel of each required declaration | Image bytes + shared prompt | Per-`DeclarationType`: value, confidence, bounding box, package face | Model-reported per detection | No fallback chain (single resolved provider) | Gemini or Claude Vision | None if the resolved provider fails — surfaces as an error |
| Image Quality Analysis | Flag unusable photos before they cause false violations | BufferedImage pixels | Score 0–100, warning set (9 types), recommended action | Deterministic heuristic — not a "confidence" in the ML sense | Heuristic thresholds, not ML-trained | None (pure Java, no model) | N/A — always runs |
| Font/Readability Analysis | Detect illegible or undersized mandatory text | Detected declaration's bounding box region | Font-size estimate (relative proxy), readability score, contrast score, issue classification | Heuristic blend (contrast 60% + edge density 40%) | Box-height proxy, not a certified mm measurement — no physical reference scale | None (heuristic) | N/A |
| AI Copilot ("Nirikshak") | Explain/summarize/recommend without deciding compliance | `CopilotContext` (post-decision facts only) + user question | Natural-language answer, tied to chat history | N/A (text generation) | Cannot access raw OCR/Vision output; cannot alter compliance state; factual questions answered deterministically, bypassing the LLM entirely | Gemini or Claude (configurable) | Deterministic keyword-matched answers for common factual questions |
| Report narrative generation | Human-readable executive summary + recommendations | Assembled `ReportData` | Narrative text for 2 fields only; rest of report is deterministic | N/A | Only 2 of many report fields are AI-generated | Same as Copilot (Gemini/Claude) | Deterministic templated text if the LLM call throws any exception |
| Prediction Engine | Forecast next-month violations/workload; flag anomalies | Historical aggregate time series | Forecast + R² + confidence interval; anomaly flags | Statistical (R², z-score), not model confidence | Classical statistics only — explicitly not LLM-based by design | None (linear regression + z-score) | N/A |
| Compliance Score / Risk Index | Single defensible numeric scores | Violations, image quality, OCR confidence, readability / aggregate violation & inspection counts | 0–100 score + band (LOW/MEDIUM/HIGH or CRITICAL-weighted) | Formula-based, not probabilistic | Weights are configurable but the formula itself is fixed | None (deterministic formula) | N/A |
| Semantic Search / Embeddings | *(planned, not built)* | — | — | — | Database schema + embedding model config exist; no consuming code anywhere | Gemini `text-embedding-004` (configured, unused) | — |

---

## SECTION 8 — PROJECT STATISTICS (all counted directly from the codebase — nothing estimated)

| Metric | Count |
|---|---|
| REST controllers | **18** |
| Total HTTP endpoints | **87** |
| Top-level backend feature packages | **21** |
| Total backend Java source files | **355** |
| Service implementation classes (`*ServiceImpl.java`) | **32** |
| Custom exception types | **6** |
| Flyway database migrations | **12** |
| Database tables (final schema) | **26** |
| Compliance rules ever defined | **21** (19 currently active, 2 deactivated/superseded) |
| Reusable rule validation strategy types | **12** |
| Backend automated tests | **68** (0 failures, verified live) |
| AI Copilot distinct capabilities | **6** (ask, explainRule, summarize, officer-notes, manufacturer-recommendations, compare) |
| Analytics + Insight endpoints | **20** (13 core analytics + 7 intelligence-layer) |
| Image-quality signals measured | **8** (blur, noise, rotation, brightness, contrast, glare, cropping, resolution) |
| Image-quality warning types | **9** |
| AI/LLM providers (pluggable) | **2** (Gemini, Claude) across both Vision AI and LLM roles |
| OCR providers | **2** (Google Vision, Tesseract) |
| Frontend source files (`.ts`/`.tsx`) | **65** |
| Frontend routes | **11** (3 real screens, 8 placeholders) |
| Frontend npm dependencies | **41** |
| Supabase Storage buckets (active / reserved) | **3 active** (`inspection-images`, `inspection-reports`, `evidence`) / 3 reserved-unused |
| Decision Trace pipeline stages tracked | **12** distinct stage names |

---

## SECTION 9 — FUTURE SCOPE (realistic only — grounded in gaps this project itself already identified)

1. **Consumer Portal** — a public-facing portal for consumers to scan/verify a product's compliance (fully designed conceptually in an earlier planning session, zero code written).
2. **Complete the Officer Portal** — Dashboard, Reports, Analytics, Rules management, Settings, and a standalone AI Copilot page currently exist only as placeholders.
3. **Semantic search** — the database (`pgvector`, `embeddings` table) and embedding model config already exist; implementing the actual embedding-generation and similarity-search service would activate previously provisioned infrastructure.
4. **Rate limiting on authentication endpoints** — explicitly identified and deferred during Phase 4's security review.
5. **Complete role-based authorization coverage** — some endpoints were flagged during the security review as missing explicit role checks.
6. **Digital signature for evidence** — the `Evidence` entity already has a reserved, unused `signature` column for this.
7. **PWA packaging** — README already states this as an intended target; no service worker/manifest work has been done yet.
8. **CI/CD pipeline** — not present in the repo currently.
9. **Enable GCP billing** — currently blocking live OCR verification on the deployed Google Cloud project (a real, currently-open item, not a hypothetical future one).
10. **Horizontal scaling / microservice extraction** — explicitly deferred by design ("zero scaling benefit at government-inspection volumes") — only relevant if real usage volume ever exceeds that assumption.

---

## SECTION 10 — PPT CONTENT (what goes on each slide — content only, not slide design)

**Slide 1 — Title**: Project name "Nirikshan AI", subtitle "AI-Powered Legal Metrology Inspection System", team name, SIH problem statement reference, one tagline from Section 1.

**Slide 2 — Problem Statement**: The Government of India Legal Metrology inspection problem (per README's framing); the core pain points this system's architecture answers — manual inspection has no photographic evidence, no audit trail, and inconsistent officer judgment. Keep this qualitative; do not cite unverified statistics.

**Slide 3 — Our Solution (Elevator Pitch)**: The 30-second elevator pitch from Section 1, plus a single diagram showing photo → OCR+Vision AI → Fusion → Rule Engine → Violations/Score → Evidence → Report.

**Slide 4 — What Makes This Different**: 4–5 rows from the Section 2 UVP table — Explainable AI boundary, Rule Engine as sole compliance authority, Evidence Engine with SHA-256 hashing, Decision Trace. This is the strongest differentiation slide — lead with "AI never decides compliance."

**Slide 5 — System Architecture**: The high-level architecture diagram (React SPA ↔ Spring Boot modular monolith ↔ PostgreSQL/Supabase Storage ↔ Google Vision/Gemini/Claude), plus the AI data-flow pipeline from Section 4.

**Slide 6 — AI Pipeline in Detail**: OCR (2 providers) + Vision AI (declaration detection) running in parallel, fusion, then the Rule Engine (19 active rules, 12 validator strategies, fully DB-driven and versioned) — emphasize this is deterministic, not AI-decided.

**Slide 7 — Explainability & Evidence**: Decision Trace (12 tracked pipeline stages, append-only) + Evidence Engine (annotated/cropped images, SHA-256 integrity hash, immutability on completion) + AI Copilot "Nirikshak" with its explicit non-decision-making boundary (quote the real system-prompt line: *"You NEVER decide, determine, override, or second-guess compliance status"*).

**Slide 8 — Live Demo / Screenshots**: Screenshots of the actual built AI Inspection Workspace (the one real, functioning screen) — capture panel, annotated bounding-box viewer, violations panel, Decision Timeline, Nirikshak chat drawer. Be explicit in speaker notes that this is the one screen built and demoed; do not show placeholder screens as if finished.

**Slide 9 — Tech Stack & Numbers**: The Section 8 statistics table (87 endpoints, 26 tables, 19 active rules, 68 passing tests, etc.) alongside the Section 3 tech stack, organized as frontend/backend/database/AI/infra.

**Slide 10 — Future Scope & Team**: Section 9's realistic future scope list (Consumer Portal, remaining Officer Portal screens, semantic search activation, PWA, CI/CD) plus team member credits.
