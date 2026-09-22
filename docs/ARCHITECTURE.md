# Legal Metrology AI Inspection System — Architecture

Government-grade system for Legal Metrology inspection officers to scan, OCR,
AI-analyze, and score packaged-commodity labels for compliance with the
Legal Metrology (Packaged Commodities) Rules, and to manage the resulting
inspection lifecycle, reporting, and analytics.

This document is the single source of truth for architecture decisions.
Application code is written against this design — nothing here is
aspirational filler.

---

## 1. Architectural Style

**Modular monolith backend + SPA/PWA frontend, contract-first over REST.**

Why not microservices: the AI pipeline is a single sequential workflow
(quality → label detection → OCR → correction → vision → rules → score →
report) that shares one transactional inspection record. Splitting it into
services now would mean distributed transactions and network hops for zero
scaling benefit at government-inspection volumes (thousands, not millions,
of inspections/day). Instead we get microservice-*readiness* for free by
strictly packaging the backend **by feature module** (Section 4) with no
module reaching into another module's repository/entity layer — only its
service's public interface. Any module (`ai`, `report`, `analytics`) can be
extracted into its own deployable later with a mechanical cut, not a rewrite.

**Layering inside every feature module** (classic clean-architecture rings,
kept thin because Spring Data / JPA already does the boring parts):

```
controller  →  service  →  repository  →  entity
     ↓             ↓
    dto      (calls other modules' service interfaces, never their repos)
```

- `controller` — HTTP boundary only. Validates request shape (`@Valid`),
  maps to/from DTOs, sets HTTP status. No business logic.
- `service` — business logic, transactions (`@Transactional`), orchestration
  across repositories and other modules' service interfaces.
- `repository` — Spring Data JPA interfaces. No business logic.
- `entity` — JPA-mapped persistence model. Never returned from a controller.
- `dto` — request/response shapes exposed over the API. Decouples the wire
  contract from the persistence model so the DB can evolve independently.
- `mapper` — MapStruct entity↔DTO conversion (compile-time generated, zero
  reflection cost, unlike ModelMapper).

**Frontend mirrors this with feature-based (not type-based) organization** —
Section 3 — because a government system with 15+ domains (inspections,
products, scans, violations, rules, reports, AI chat, analytics, admin...)
becomes unnavigable if you group by file *type* across the whole app instead
of by *feature*. Cross-feature primitives live in top-level `components/`,
`hooks/`, `services/`.

---

## 2. High-Level System Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│  CLIENT                                                              │
│  React + Vite PWA (installable, offline-capable shell)               │
│  - Officer scans a product in the field (camera / barcode / QR)      │
│  - TanStack Query caches + syncs; service worker queues offline POSTs│
└───────────────┬───────────────────────────────────────────────────────┘
                │ HTTPS + JWT (Supabase-issued, verified by Spring)
┌───────────────▼───────────────────────────────────────────────────────┐
│  SPRING BOOT API  (stateless, horizontally scalable)                  │
│  Spring Security filter chain validates Supabase JWT (JWKS)           │
│  ┌────────────┐ ┌───────────┐ ┌────────────┐ ┌───────────────────┐   │
│  │ auth/user  │ │ product/  │ │ inspection/│ │ ai/ (pipeline      │   │
│  │            │ │ scanner/  │ │ image/     │ │  orchestrator +    │   │
│  │            │ │           │ │ history/   │ │  ocr/vision/llm/   │   │
│  │            │ │           │ │            │ │  rules/embedding)  │   │
│  └────────────┘ └───────────┘ └────────────┘ └───────────────────┘   │
│  ┌────────────┐ ┌───────────┐ ┌────────────┐                         │
│  │ analytics/ │ │ report/   │ │ chat/       │  cross-cutting:        │
│  │ dashboard/ │ │ (PDF/XLS) │ │ settings/   │  security/ config/     │
│  │            │ │           │ │ audit/      │  exception/ validation/│
│  └────────────┘ └───────────┘ └────────────┘  common/                │
└───────────────┬──────────────────────────┬──────────────────────────┘
                │                          │
   ┌────────────▼───────────┐   ┌──────────▼───────────────────────┐
   │ Supabase Postgres       │   │ External AI providers            │
   │ (+ pgvector extension)  │   │ Google Vision OCR, Gemini Vision, │
   │ Row-level security OFF  │   │ Gemini 2.5 Pro / Claude (LLM),    │
   │ at DB — API is the      │   │ Tesseract (offline OCR fallback)  │
   │ trust boundary          │   └───────────────────────────────────┘
   └────────────┬────────────┘
   ┌────────────▼────────────┐
   │ Supabase Storage         │
   │ buckets: images/ reports/│
   │ temp/ exports/           │
   │ training-data/           │
   └───────────────────────────┘
```

**Auth model**: Supabase Auth issues the JWT (email/password, and later
OTP/SSO for officers). The Spring backend does **not** re-implement login —
it validates Supabase-issued JWTs against Supabase's JWKS endpoint via a
custom `JwtAuthFilter`, extracts `sub` (user id) and a custom `role` claim,
and applies method-level `@PreAuthorize` RBAC from there. Supabase is the
identity provider; Spring Security is the authorization/PEP layer. This
avoids maintaining a second password store and lets the frontend use
`supabase-js` directly for login/refresh while all *business* API calls go
to the Spring backend.

---

## 3. Frontend Structure (React + Vite + TypeScript)

```
frontend/
  public/
    icons/                       PWA icons (192/512/maskable), favicon
    manifest.webmanifest
  src/
    assets/                      images, icons, fonts bundled at build time
    components/
      ui/                        shadcn/ui primitives (button, dialog, table…)
      common/                    Navbar, Sidebar, PageHeader, DataTable,
                                  EmptyState, ErrorBoundary, ConfirmDialog,
                                  StatusBadge, SeverityBadge, LoadingSkeletons
      charts/                    Recharts wrappers: ComplianceGauge,
                                  ViolationTrendChart, RegionHeatmap,
                                  CategoryBarChart, TrendSparkline
      forms/                     FormField, FileUploadField, CameraCaptureField,
                                  MultiImageDropzone
      scanners/                  BarcodeScannerView, QrScannerView
                                  (wrap ZXing, expose one onDetect(value) API)
      layout-parts/               TopBar, BreadcrumbBar, CommandPalette
    features/                    <-- one folder per domain, see below
      auth/
      dashboard/
      inspections/
      products/
      scans/
      violations/
      rules/
      reports/
      ai-assistant/               copilot chat panel
      ai-vision/                  bounding-box viewer, label segmentation UI
      analytics/
      history/
      settings/
      admin-users/
      admin-roles/
      offline-sync/               PWA offline queue UI/status
        components/  hooks/  api/  types/  schemas/
    hooks/                       useDebounce, useMediaQuery, usePagination,
                                  useLocalStorage, useCamera, useOnlineStatus
    contexts/                    AuthContext, ThemeContext, LocaleContext
    layouts/                     AppLayout, AuthLayout, DashboardLayout,
                                  MobileScanLayout
    routes/                      AppRouter.tsx, ProtectedRoute.tsx,
                                  routeConfig.ts, roleGuard.ts
    services/                    apiClient.ts (axios + interceptors),
                                  supabaseClient.ts, storageService.ts,
                                  websocketService.ts (live dashboard updates)
    utils/                       formatters.ts, dateUtils.ts, fileUtils.ts,
                                  imageUtils.ts, exportUtils.ts,
                                  complianceUtils.ts
    constants/                   routes.ts, roles.ts, ruleCategories.ts,
                                  severityLevels.ts, queryKeys.ts
    types/                       api.types.ts, domain.types.ts, auth.types.ts
    styles/                      globals.css, tailwind theme tokens
    i18n/                        locales/{en,hi,mr,ta,gu}/*.json
    lib/                         cn.ts (shadcn class merge), queryClient.ts
    App.tsx  main.tsx
```

**Feature module internal contract** (every folder under `features/*`
follows the same shape, so any engineer can navigate any feature blind):

- `components/` — feature-only UI, never imported by another feature.
- `hooks/` — feature-only hooks (mostly thin TanStack Query wrappers).
- `api/` — the feature's HTTP calls (`inspectionsApi.ts` etc.), the *only*
  place `apiClient` is called from within that feature.
- `types/` — feature-local TypeScript types.
- `schemas/` — Zod schemas, shared 1:1 with React Hook Form resolvers and
  reused to validate data at the API boundary (parse, don't just type-check).

**Why feature-based over type-based (`pages/components/hooks` globally)**:
a government inspection app has 15+ verticals; type-based grouping means
every change touches 4 unrelated top-level folders and nothing signals
which files belong together. Feature-based keeps blast radius local and
scales to more officers/domains being added later without restructuring.

---

## 4. Backend Structure (Spring Boot, Java 21, Maven)

```
backend/src/main/java/gov/legalmetrology/inspection/
  config/          SecurityConfig, SwaggerConfig, CorsConfig, AsyncConfig,
                   AiProviderConfig, SupabaseStorageConfig, JacksonConfig
  security/        JwtAuthFilter, SupabaseJwtValidator, UserPrincipal,
                   SecurityUtils, @HasRole annotation
  exception/       GlobalExceptionHandler (@RestControllerAdvice),
                   ResourceNotFoundException, ValidationException,
                   AiServiceException, StorageException
  validation/      @ValidBarcode, @ValidMrp, @ValidImageType validators
  common/
    dto/           ApiResponse<T>, PagedResponse<T>, ErrorResponse
    entity/        BaseEntity (id, createdAt, updatedAt, createdBy) — MappedSuperclass
    mapper/        MapStruct central config (component model = spring)
    util/          DateUtils, HashUtils, PdfUtils, ExcelUtils, FileUtils
    constant/      RoleNames, SeverityLevel, InspectionStatus (shared enums)

  auth/            controller/ service/ dto/          — session bootstrap,
                   profile sync from Supabase Auth webhook, current-user info
  user/            controller/ service/ repository/ entity/ dto/ mapper/
                                                        — officers, roles, offices
  product/         controller/ service/ repository/ entity/ dto/ mapper/
                                                        — products, categories,
                                                          manufacturers, barcode registry
  scanner/         controller/ service/ dto/           — barcode/QR lookup,
                                                          "barcode intelligence"
                                                          (prior inspection history)
  inspection/      controller/ service/ repository/ entity/ dto/ mapper/
                                                        — inspection lifecycle,
                                                          orchestrates the AI pipeline
  image/           controller/ service/ repository/ entity/ dto/ mapper/
                                                        — upload handling,
                                                          Supabase Storage integration
  ai/
    pipeline/      InspectionPipelineOrchestrator — runs the 12-stage pipeline
                   (Section 6), one stage = one injected service, saga-style
                   with per-stage status persisted so a failed stage is
                   retryable without re-running earlier (paid) stages
    ocr/           OcrService (facade) + provider/ (GoogleVisionOcrProvider,
                   TesseractOcrProvider) + OcrCorrectionService (LLM-based)
    vision/        VisionAiService (facade) + provider/ (GeminiVisionProvider,
                   ClaudeVisionProvider), ImageQualityService,
                   LabelDetectionService, FontAnalysisService,
                   FakeLabelDetectionService, DuplicateDetectionService,
                   LabelSegmentationService
    llm/           LlmService (facade) + provider/ (GeminiLlmProvider,
                   ClaudeLlmProvider) + prompt/ (versioned prompt templates),
                   ComplianceAssistantService, InspectionNoteGeneratorService,
                   RuleExplanationService, RecommendationEngineService,
                   VoiceCommandService
    rules/         RuleEngineService, RuleRepository/Entity, ViolationDetectionService,
                   SeverityClassificationService, FraudRiskScoringService,
                   ComplianceScoreService
    embedding/     EmbeddingService, EmbeddingRepository (pgvector),
                   SemanticSearchService
    dto/           BoundingBox, ConfidenceScore, AiExplanation, PipelineStageResult
  analytics/       controller/ service/ repository/ dto/
                                                        — trend/heatmap/region/
                                                          company aggregation queries
  dashboard/       controller/ service/ dto/           — summary cards, recent
                                                          activity feed
  report/          controller/ service/ repository/ entity/ dto/
                                                        — PdfReportGenerator (OpenPDF),
                                                          ExcelReportGenerator (Apache POI),
                                                          template rendering
  history/         controller/ service/ repository/ entity/ dto/
                                                        — inspection status history,
                                                          audit trail read API
  chat/            controller/ service/ repository/ entity/ dto/
                                                        — AI copilot conversation log
  settings/        controller/ service/ repository/ entity/ dto/
                                                        — system config (thresholds,
                                                          feature flags, locales)
  audit/           controller/ service/ repository/ entity/ dto/
                                                        — write-only audit log,
                                                          populated by an AOP aspect
                                                          around all mutating endpoints
```

**Module boundary rule**: a module may depend on another module's `service`
interface (constructor-injected) but must **never** import another module's
`repository` or `entity`. This is enforced by convention now and can be
enforced mechanically later with ArchUnit tests once the team grows.

---

## 5. Database Schema (Supabase PostgreSQL)

Normalized to 3NF. `pgvector` extension enabled for semantic search
embeddings. Auth users live in Supabase's own `auth.users`; our schema
extends identity via `profiles` (1:1, FK to `auth.users.id`).

### Entity groups

**Identity & access**
- `profiles` — id (=auth.users.id, PK), full_name, employee_code, office_id (FK), phone, preferred_locale, is_active
- `roles` — id, name (`INSPECTOR`, `SENIOR_INSPECTOR`, `OFFICE_ADMIN`, `SYSTEM_ADMIN`), description
- `permissions` — id, code, description
- `role_permissions` — role_id (FK), permission_id (FK) — composite PK
- `user_roles` — user_id (FK → profiles), role_id (FK) — composite PK (a user can hold >1 role)
- `offices` — id, name, region, state, jurisdiction_code

**Product domain**
- `manufacturers` — id, name, gstin, address, region
- `product_categories` — id, name, parent_category_id (self-FK, for nested categories)
- `products` — id, name, category_id (FK), manufacturer_id (FK), barcode (unique, indexed), default_unit, created_by (FK)
- `barcode_lookups` — id, barcode, product_id (FK, nullable), lookup_count, last_seen_at — powers "AI Barcode Intelligence"

**Inspection domain**
- `inspections` — id, inspector_id (FK → profiles), office_id (FK), product_id (FK, nullable — may be unknown at scan time), status (`DRAFT|IN_PROGRESS|PENDING_REVIEW|COMPLETED|CLOSED`), location (lat/lng), compliance_score (0–100, nullable until pipeline completes), fraud_risk (`LOW|MEDIUM|HIGH`, nullable), started_at, completed_at
- `inspection_status_history` — id, inspection_id (FK), from_status, to_status, changed_by (FK), changed_at, note
- `scans` — id, inspection_id (FK), scan_type (`BARCODE|QR|MANUAL`), scan_value, scanned_at

**Images & AI detections**
- `images` — id, inspection_id (FK), storage_path, image_type (`FRONT|BACK|SIDE|OTHER`), quality_score, quality_issues (jsonb: blurry/dark/cropped/rotated/glare flags), perceptual_hash (for duplicate detection), uploaded_at
- `ocr_results` — id, image_id (FK), provider (`GOOGLE_VISION|TESSERACT`), raw_text, corrected_text, confidence, created_at
- `vision_detections` — id, image_id (FK), declaration_type (`MRP|NET_QUANTITY|MFG_DATE|EXPIRY_DATE|FSSAI|MANUFACTURER_NAME|CONSUMER_CARE|COUNTRY_OF_ORIGIN|UNIT_SALE_PRICE`, etc.), detected_value, confidence, bounding_box (jsonb: x,y,w,h), is_present (bool), font_size_estimate, readability_score

**Rules & compliance**
- `rules` — id, rule_code (unique), title, description, legal_reference, category, default_severity (`CRITICAL|MAJOR|MINOR`), is_active
- `rule_parameters` — id, rule_id (FK), param_key, param_value — configurable thresholds (e.g. min font size mm) without redeploying
- `violations` — id, inspection_id (FK), rule_id (FK), severity, description, ai_explanation, confidence, status (`OPEN|ACKNOWLEDGED|RESOLVED`), created_at

**AI artifacts**
- `ai_explanations` — id, ref_type (`VIOLATION|INSPECTION`), ref_id, prompt, response, model, token_count, latency_ms, created_at — full audit trail of every LLM call, for cost tracking and reproducibility
- `embeddings` — id, ref_type (`INSPECTION|PRODUCT|VIOLATION`), ref_id, content, embedding `vector(768)`, created_at — pgvector, ivfflat index, powers semantic search
- `chat_logs` — id, user_id (FK), inspection_id (FK, nullable), role (`USER|ASSISTANT`), message, created_at

**Reporting & ops**
- `compliance_reports` — id, inspection_id (FK), report_number (unique, human-readable), pdf_path, excel_path, generated_by (FK), generated_at
- `audit_logs` — id, user_id (FK, nullable), action, entity_type, entity_id, metadata (jsonb), ip_address, created_at
- `settings` — id, key (unique), value (jsonb), updated_by (FK), updated_at

Full DDL with constraints, indexes, and the pgvector index lives in
[`database/schema.sql`](../database/schema.sql).

### Key relationships (cardinality)
- office 1—* profiles; profiles *—* roles via user_roles
- manufacturer 1—* products; category 1—* products (category is self-referencing for subcategories)
- inspection *—1 product, *—1 inspector, *—1 office; inspection 1—* scans, 1—* images, 1—* violations, 1—* status_history
- image 1—* ocr_results (one per OCR provider attempt), 1—* vision_detections
- violation *—1 rule; rule 1—* rule_parameters
- inspection 1—0/1 compliance_report

### Indexing strategy
- B-tree on every FK.
- Unique index on `products.barcode`, `rules.rule_code`, `compliance_reports.report_number`.
- Composite index `(inspection_id, status)` on `violations` for fast dashboard filtering.
- GIN index on `audit_logs.metadata` and `settings.value` (jsonb).
- `ivfflat` index on `embeddings.embedding` for approximate nearest-neighbor semantic search.
- Partial index on `inspections (status) WHERE status != 'CLOSED'` — the dashboard's hottest query.

---

## 6. Storage Structure (Supabase Storage)

Five buckets, private by default, accessed only via signed URLs issued by
the backend after an authorization check (never expose the service key to
the client):

```
images/{inspectionId}/{imageType}/{uuid}.{ext}          officer-captured label photos
reports/{inspectionId}/{reportNumber}.pdf                generated PDF reports
reports/{inspectionId}/{reportNumber}.xlsx               generated Excel exports
temp/{sessionId}/{uuid}.{ext}                            in-progress uploads before an
                                                          inspection record exists;
                                                          swept by a scheduled cleanup job
exports/{yyyy-MM-dd}/{exportId}.xlsx                     bulk dashboard/analytics exports
training-data/{category}/{uuid}.{ext} + .json sidecar    officer-confirmed AI corrections,
                                                          curated for future model fine-tuning
```

Retention: `temp/` purged after 24h via a scheduled job in `settings`-driven
config; everything else retained per the statutory record-keeping period.

---

## 7. AI Pipeline Architecture

The pipeline is not "just OCR" — it's a 12-stage orchestrated workflow,
implemented as `InspectionPipelineOrchestrator` in `ai/pipeline/`. Each
stage writes a `PipelineStageResult` (status, output, confidence, latency,
cost) to the inspection record, so the pipeline is resumable, explainable,
and auditable end-to-end — not a black box.

```
1. Image Upload             → image/ module: validate size/type, push to
                               Supabase Storage `images/`, create `images` row

2. Image Quality Analysis    → ai/vision: ImageQualityService detects blur,
                               low light, cropping, rotation, glare
                               (OpenCV: Laplacian variance for blur, histogram
                               for exposure, edge/contour heuristics for crop/
                               rotation). Score < threshold → request re-capture
                               *before* spending OCR/LLM budget.

3. Label Detection/Segmentation → LabelDetectionService (Gemini Vision) finds
                               the label region(s) in-frame and classifies
                               front/back/side (LabelSegmentationService),
                               tagging `images.image_type`.

4. OCR                       → OcrService facade: GoogleVisionOcrProvider
                               (primary) with automatic fallback to
                               TesseractOcrProvider on API failure/quota —
                               strategy pattern, provider chosen by
                               AiProviderConfig, never hardcoded.

5. OCR Correction             → OcrCorrectionService: raw OCR text has
                               digit/unit confusions (e.g. "1O0g" → "100g").
                               An LLM pass (Gemini/Claude, prompt in
                               llm/prompt/) corrects text using label-domain
                               context, storing both raw and corrected text
                               for audit.

6. Vision AI (declaration detection)  → VisionAiService: given the corrected
                               text + image, detects each mandatory
                               declaration (MRP, net quantity, mfg/expiry
                               date, FSSAI license, manufacturer name/address,
                               consumer care, country of origin, unit sale
                               price), each with a confidence score and
                               bounding box → `vision_detections` rows.
                               Also runs FontAnalysisService (readability,
                               estimated font size vs. legal minimum) and
                               FakeLabelDetectionService (tamper/edit
                               artifact detection) in parallel.

7. Rule Engine                → RuleEngineService: evaluates `rules` +
                               `rule_parameters` against the structured
                               detections. Deterministic, versioned,
                               explainable — NOT an LLM call. This is what
                               makes the system legally defensible: a human
                               can read exactly which rule fired and why.

8. Violation Detection         → ViolationDetectionService persists
                               `violations` rows for every failed rule, with
                               SeverityClassificationService assigning
                               CRITICAL/MAJOR/MINOR per rule metadata.

9. Compliance Score            → ComplianceScoreService: weighted scoring
                               (severity-weighted) → 0–100 score;
                               FraudRiskScoringService derives LOW/MEDIUM/HIGH
                               from score + fake-label signal + duplicate-image
                               signal + historical manufacturer pattern
                               (via embeddings/semantic lookup).

10. Report Generation          → report/ module: PdfReportGenerator (OpenPDF)
                               + ExcelReportGenerator (Apache POI) render the
                               inspection into a professional report using a
                               templates/reports/ layout.

11. AI Explanation              → ComplianceAssistantService (LLM) generates
                               natural-language explanations per violation
                               ("why this failed", "how to fix") and
                               RecommendationEngineService drafts
                               manufacturer-facing remediation guidance.
                               Every call logged to `ai_explanations`.

12. Dashboard Analytics         → analytics/ module aggregates completed
                               inspections into trend/heatmap/region/company
                               views (materialized via scheduled queries,
                               not computed live, to keep the dashboard fast).
```

Each stage is a Spring bean implementing a common `PipelineStage` interface
so stages can be reordered, A/B tested, or replaced (e.g., swapping in
YOLOv11 object detection as a pre-stage 3 once trained) without touching the
orchestrator.

### Cross-cutting AI capabilities (not pipeline stages, called on demand)
- **Semantic Search** (`embedding/SemanticSearchService`) — natural-language
  queries ("products with missing MRP", "biscuits inspected in July")
  embedded via the LLM provider's embedding endpoint, matched against
  `embeddings` with pgvector cosine similarity, re-ranked by structured
  filters (date/category) pushed down to SQL.
- **AI Copilot Chat** (`chat/` + `llm/ComplianceAssistantService`) —
  RAG over the specific inspection's violations + rule text + prior
  chat_logs as context window.
- **Duplicate Detection** — perceptual hash comparison (`images.perceptual_hash`)
  is the cheap first pass; only ambiguous matches escalate to a vision-model
  similarity call.
- **Barcode Intelligence** (`scanner/`) — on scan, looks up `barcode_lookups`
  → `products` → prior `inspections` so an officer sees history instantly.
- **Voice Assistant** — browser Web Speech API on the frontend for
  capture; transcribed command text is routed through the same LLM facade
  as chat, with a small fixed intent set (navigate, start scan, read score).
- **Multilingual** — frontend `i18n/` (react-i18next) for UI strings;
  AI-generated text (explanations, recommendations) requested from the LLM
  in the officer's `preferred_locale` directly via the prompt template, not
  machine-translated after the fact (higher quality, one call).

### Provider abstraction (why)
`ocr/provider/`, `vision/provider/`, `llm/provider/` each define a small
interface (`OcrProvider`, `VisionProvider`, `LlmProvider`) with concrete
implementations selected by config (`ai.ocr.provider=google-vision`,
`ai.llm.provider=gemini`). This lets Gemini↔Claude be swapped per
environment or per call (e.g., Claude Vision as a second opinion on
low-confidence detections) without touching business logic — required
given the spec's "Claude API configurable" instruction.

---

## 8. API Structure

Base path: `/api/v1`. JSON everywhere. Every response wrapped in
`ApiResponse<T>` (`{ success, data, error, timestamp }`); paginated list
endpoints wrapped in `PagedResponse<T>` (`{ items, page, size, totalItems, totalPages }`).
Errors follow RFC 7807-flavored `ErrorResponse` (`code`, `message`, `details`, `traceId`).

| Module | Representative endpoints |
|---|---|
| auth | `GET /auth/me`, `POST /auth/sync-profile` |
| user | `GET/POST/PATCH /users`, `GET /users/{id}`, `GET/POST /roles`, `POST /users/{id}/roles` |
| product | `GET/POST /products`, `GET /products/{id}`, `GET /product-categories`, `GET /manufacturers` |
| scanner | `POST /scan/barcode`, `POST /scan/qr`, `GET /scan/barcode/{code}/history` |
| inspection | `POST /inspections`, `GET /inspections`, `GET /inspections/{id}`, `PATCH /inspections/{id}/status`, `GET /inspections/{id}/history` |
| image | `POST /inspections/{id}/images` (multipart), `GET /images/{id}`, `DELETE /images/{id}` |
| ai (pipeline) | `POST /inspections/{id}/pipeline/run`, `GET /inspections/{id}/pipeline/status`, `POST /inspections/{id}/pipeline/stages/{stage}/retry` |
| ai (vision) | `GET /images/{id}/detections`, `GET /images/{id}/quality` |
| ai (assistant) | `POST /chat`, `GET /chat/{inspectionId}` |
| ai (search) | `POST /search/semantic` |
| rules | `GET/POST/PATCH /rules`, `GET /rules/{id}/parameters` |
| violations | `GET /inspections/{id}/violations`, `PATCH /violations/{id}/status`, `GET /violations/{id}/explanation` |
| report | `POST /inspections/{id}/reports/pdf`, `POST /inspections/{id}/reports/excel`, `GET /reports/{id}/download` |
| analytics | `GET /analytics/trends`, `GET /analytics/heatmap`, `GET /analytics/by-region`, `GET /analytics/by-company` |
| dashboard | `GET /dashboard/summary`, `GET /dashboard/recent-activity` |
| history | `GET /audit-logs`, `GET /inspections/{id}/status-history` |
| settings | `GET/PATCH /settings` |

Full request/response schemas are generated automatically from the code via
**springdoc-openapi** and served at `/swagger-ui.html` — the OpenAPI spec
itself is the contract, not a hand-maintained doc, so it can never drift.

---

## 9. Reusable Components (Frontend)

- `DataTable` — generic, sortable/filterable/paginated table (wraps
  TanStack Table) used by inspections list, products list, users list,
  audit logs.
- `StatusBadge` / `SeverityBadge` — consistent color-coded chips for
  inspection status and violation severity across the whole app.
- `ComplianceGauge` — Recharts radial gauge for the 0–100 score, reused on
  inspection detail, dashboard cards, and PDF preview.
- `BoundingBoxOverlay` — draws AI-detected bounding boxes over an
  `<img>`/`<canvas>`, used by both the vision-detection review screen and
  the "highlight violations" view.
- `FileUploadField` / `CameraCaptureField` — RHF-integrated, handle
  multi-image upload with client-side compression before hitting Supabase
  Storage.
- `BarcodeScannerView` / `QrScannerView` — thin wrapper around ZXing's
  camera stream, emits a single `onDetect(value)` callback so scanner UX is
  identical everywhere it's used (new inspection, product lookup, admin).
- `ChatPanel` — the AI Copilot UI, reused as a full page and as a
  collapsible drawer on the inspection detail screen.
- `EmptyState`, `ErrorBoundary`, `ConfirmDialog`, `LoadingSkeletons` —
  standard states every list/detail screen needs.

## 10. Services (Frontend `services/` + Backend module `service/`)

Frontend: `apiClient` (axios instance with JWT-attach + 401 refresh
interceptor), `supabaseClient` (auth + storage signed-upload helper),
`websocketService` (STOMP/SockJS client for live dashboard counters),
`storageService` (client-side image compression before upload).

Backend: one `*Service` per module (Section 4) plus the AI facades
(`OcrService`, `VisionAiService`, `LlmService`) which hide provider choice
behind a stable interface consumed by the pipeline orchestrator and by
on-demand endpoints alike.

## 11. Utilities

Frontend `utils/`: `formatters.ts` (currency/date/number for Indian
locale — ₹, DD/MM/YYYY), `imageUtils.ts` (client-side resize/compress
before upload), `exportUtils.ts` (trigger file download from a blob),
`complianceUtils.ts` (score→color/label mapping shared by gauge and badges).

Backend `common/util/`: `PdfUtils`/`ExcelUtils` (OpenPDF/Apache POI
helpers), `HashUtils` (perceptual + SHA-256 hashing for images), `DateUtils`
(UTC storage, IST display), `FileUtils` (extension/mime validation).

## 12. Naming Conventions

- **Frontend files**: `PascalCase.tsx` for components, `camelCase.ts` for
  hooks/utils/services, hooks always prefixed `use*`. Feature API files:
  `{feature}Api.ts`. Zod schemas: `{entity}Schema.ts`. Query keys centralized
  in `constants/queryKeys.ts` as typed factory functions, never inline
  string arrays, so TanStack Query cache invalidation can't typo-drift.
- **Backend**: standard Java conventions — `PascalCase` classes,
  `camelCase` methods/fields, REST paths `kebab-case` and plural nouns
  (`/rule-parameters`), DTOs suffixed `Request`/`Response` explicitly
  (`CreateInspectionRequest`, `InspectionResponse`), MapStruct interfaces
  suffixed `Mapper`.
- **Database**: `snake_case` tables/columns, tables plural
  (`inspections`), FKs named `{referenced_table_singular}_id`
  (`inspection_id`), join tables named `{a}_{b}` alphabetical
  (`role_permissions`).
- **Git**: Conventional Commits (`feat:`, `fix:`, `chore:`) — enables
  automated changelog generation later.

## 13. Environment Variables

```
# --- Frontend (.env) ---
VITE_API_BASE_URL=
VITE_SUPABASE_URL=
VITE_SUPABASE_ANON_KEY=
VITE_WS_URL=
VITE_DEFAULT_LOCALE=en

# --- Backend (application-{profile}.yml, sourced from env in prod) ---
SPRING_DATASOURCE_URL=                # Supabase Postgres connection string
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
SUPABASE_URL=
SUPABASE_SERVICE_ROLE_KEY=            # server-side only, never shipped to client
SUPABASE_JWT_SECRET=                  # or JWKS URL, for JwtAuthFilter
SUPABASE_STORAGE_BUCKET_IMAGES=images
SUPABASE_STORAGE_BUCKET_REPORTS=reports
SUPABASE_STORAGE_BUCKET_TEMP=temp
SUPABASE_STORAGE_BUCKET_EXPORTS=exports
SUPABASE_STORAGE_BUCKET_TRAINING=training-data

AI_OCR_PROVIDER=google-vision         # google-vision | tesseract
GOOGLE_VISION_API_KEY=
AI_VISION_PROVIDER=gemini             # gemini | claude
AI_LLM_PROVIDER=gemini                # gemini | claude
GEMINI_API_KEY=
GEMINI_MODEL=gemini-2.5-pro
ANTHROPIC_API_KEY=
CLAUDE_MODEL=

APP_JWT_ISSUER=
APP_CORS_ALLOWED_ORIGINS=
APP_LOG_LEVEL=INFO
```

Secrets are never committed — `.env` / `application-local.yml` are
git-ignored; production values live in the deploy platform's secret store.

## 14. Libraries — Chosen & Why

**Frontend**
| Library | Why |
|---|---|
| React + Vite | fast HMR, native ESM dev server, smaller/faster prod builds than CRA |
| TypeScript | catches contract drift between frontend and the OpenAPI-generated backend types |
| Tailwind CSS | utility-first, no CSS file sprawl across 15+ feature folders, trivial dark-mode via `dark:` variants |
| shadcn/ui | accessible Radix-based primitives you own the source of — critical for a government app that must meet accessibility standards and needs custom theming, unlike a black-box component library |
| React Router | standard, data-router APIs support role-guarded route loaders |
| TanStack Query | server-state caching/sync/retry/offline-queue semantics fit for a field-use PWA far better than hand-rolled `useEffect` fetching |
| React Hook Form + Zod | uncontrolled-input performance for image-heavy forms; Zod schemas double as runtime API-boundary validation and RHF resolvers — one schema, two jobs |
| Framer Motion | declarative, physics-based transitions for a "modern, government-grade, minimal" feel without hand-written CSS keyframes |
| Recharts | composable, SVG-based, themeable — fits the dashboard's trend/heatmap/gauge needs and integrates cleanly with Tailwind tokens |

**Backend**
| Library | Why |
|---|---|
| Spring Boot 3 / Java 21 | virtual threads (Project Loom) make the many blocking I/O calls in the AI pipeline (OCR/vision/LLM HTTP calls) cheap without a reactive rewrite |
| Spring Security + JWT | validates Supabase-issued JWTs; method-level `@PreAuthorize` gives declarative RBAC per endpoint |
| Spring Data JPA | repository boilerplate elimination, works natively with Supabase Postgres |
| springdoc-openapi (Swagger) | generates the API contract from code — can't drift from the real endpoints |
| MapStruct | compile-time entity↔DTO mapping, no reflection overhead vs. ModelMapper |
| Flyway | versioned, reviewable SQL migrations — required for a schema this size to stay reproducible across environments |
| OpenPDF | actively maintained iText 4 fork, LGPL (iText 5+ is AGPL/commercial) — avoids licensing risk for a government deployment |
| Apache POI | de facto standard for `.xlsx` generation, needed for the mandated Excel export |
| ZXing | server-side barcode/QR decode fallback when client-side camera decode is ambiguous; also used client-side via `@zxing/library` |
| Resilience4j | circuit breaker/retry around every external AI provider call — a stalled Gemini/Vision API must not take the pipeline down |
| Testcontainers | integration tests against a real Postgres+pgvector container, not mocks, for schema-sensitive code |

**AI/Data**
| Library | Why |
|---|---|
| Google Vision API | highest OCR accuracy on printed packaging text at varied angles/lighting — primary OCR |
| Tesseract (via Tess4J) | free, offline fallback when Vision API is unavailable/over quota — keeps field inspections working without connectivity |
| Gemini 2.5 Pro | primary LLM/vision-LLM: multimodal (handles image+text in one call for vision detection), strong structured-output mode for reliable JSON extraction |
| Claude API | configurable second provider for LLM/vision — used as a swappable alternative and as a second-opinion check on low-confidence detections |
| pgvector | keeps embeddings colocated with relational data in the same Supabase Postgres instance — no separate vector DB to operate |
| OpenCV (via Bytedeco JavaCV) | deterministic, fast, free image-quality heuristics (blur/exposure/crop/rotation) that shouldn't cost an LLM call per image |
| YOLOv11 (future) | planned object-detection stage for label localization once a labeled training set exists (fed by the `training-data/` bucket) — pipeline's `PipelineStage` interface already accommodates inserting it |

## 15. Scalability & Future Expansion

- **Module extraction**: the `ai/` module is the most compute/cost-heavy
  and the most likely to need independent scaling (GPU inference, provider
  rate limits). Because it's already isolated behind service interfaces
  with no other module touching its repositories, it can become its own
  Spring Boot service behind the same API gateway with a mechanical move,
  not a rewrite.
- **Async pipeline**: `InspectionPipelineOrchestrator` runs stages via
  Spring's `@Async`/virtual threads today; the `PipelineStageResult`
  persistence model means it can move to a real message queue (e.g., a
  Postgres-backed outbox → SQS/RabbitMQ) later without changing the stage
  contracts.
- **Read scaling**: `analytics/` and `dashboard/` queries are aggregation-
  heavy; designed from day one to read from scheduled materialized views
  rather than live-aggregating `inspections`/`violations`, so dashboard load
  never blocks inspection writes and can later point at a read replica.
- **Multi-tenancy readiness**: every domain table already carries
  `office_id`/`region` — a future state-level rollout with per-state data
  isolation is a `WHERE` clause and a Postgres RLS policy away, not a schema
  migration.
- **i18n/AI multilingual**: locale is a first-class column
  (`profiles.preferred_locale`) and a prompt-template parameter, so adding
  a 6th language is a translation file + prompt variant, not a code change.
- **Offline-first PWA**: `features/offline-sync/` and the service worker
  queue are designed in from the start (not bolted on) because field
  inspection connectivity is unreliable by nature of the problem domain.
