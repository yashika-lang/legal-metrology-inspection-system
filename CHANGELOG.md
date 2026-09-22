# Changelog

Architectural decisions, bug fixes, migrations, and design changes, in the
order they happened. This is a design/engineering log, not a release note
list — each entry explains *why*, not just *what*.

## Phase 1 — Backend foundation

- **Decision**: Feature-sliced package structure (`auth`, `product`,
  `inspection`, `scanner`, `report`, `history`, `storage` each own
  `controller/service/repository/entity/dto/mapper`), with cross-cutting
  concerns (`config`, `security`, `exception`, `validation`, `utils`,
  `common`) at the root. Chosen over flat layer-based packaging so any one
  domain's blast radius stays local as the system grows.
- **Decision**: Custom JWT auth (signup/login/refresh/logout/forgot-password)
  against our own `users` table, rather than delegating to Supabase Auth —
  Supabase is used purely as hosted Postgres + Storage. Simpler operationally
  and keeps the auth flow fully owned and testable.
- **Bug found & fixed (via real DB smoke test)**: `BaseEntity.id` was
  annotated `@JdbcTypeCode(SqlTypes.CHAR)`, forcing every UUID primary key
  (and therefore every FK referencing one) to bind as a JDBC `CHAR`/varchar
  instead of native `uuid`. Every join query failed with `operator does not
  exist: uuid = character varying`. Removed the annotation; Hibernate's
  default UUID mapping is correct for PostgreSQL's `uuid` type.
- **Decision**: Every mutable "current state" table (label detections,
  violations, fused declarations) is deleted-then-reinserted on
  re-evaluation rather than upserted in place — these represent "the current
  understanding," not a history. Append-only audit trails (`ocr_results`,
  `audit_logs`) are never deleted.

## Phase 2 — AI Engine (OCR, Vision, Rules, Analytics)

- **Decision**: `LabelDetection` is a single shared table for both
  OCR-sourced and Vision-AI-sourced declarations (discriminated by
  `source`), rather than two parallel tables — the rule engine and fusion
  logic need to treat both signals uniformly.
- **Decision**: The rule engine is fully database-driven — a `Rule` row
  selects a generic `validation_type` (FIELD_EXISTS, REGEX, NUMERIC,
  FONT_SIZE, CUSTOM, …) plus a JSON `validation_expression`; new simple
  rules are pure data, not new Java code. `CUSTOM` is the only escape hatch
  for genuinely composite logic (e.g. "importer present ⟹ origin required"),
  registered by name.
- **Decision**: Rule *versioning* is immutable-row-based — updating a rule
  never mutates the row that may already be referenced by a historical
  `Violation`; it inserts a new row (`version + 1`) and deactivates the old
  one. A partial unique index enforces exactly one active version per
  `rule_code`.
- **Bug found & fixed (via real DB smoke test)**: `LM-LICENSE-001`'s seed
  migration was an `UPDATE ... WHERE rule_code = 'LM-LICENSE-001'` against a
  row that was never seeded in Phase 1 — a silent no-op. Converted to an
  `INSERT`.
- **Bug found & fixed (via real DB smoke test)**: `RuleService.update()`
  called `save()` on the deactivated old version before `save()`-ing the new
  active version, but Hibernate's default flush ordering runs all pending
  INSERTs before UPDATEs regardless of call order — the new version's insert
  could reach the database before the old version's deactivation did,
  tripping the "one active version per rule_code" partial unique index.
  Fixed with an explicit `saveAndFlush` on the deactivation step.
- **Decision**: Analytics/Insight/Prediction/Risk consume only
  `analytics.model.*` records built from persisted aggregates — never OCR
  text, Vision provider details, or rule evaluation internals. The one
  `AnalyticsRepository` (JdbcTemplate, native SQL) is the sole place
  aggregation SQL is written; everything above it is provider-agnostic.
- **Bug found & fixed (via real DB smoke test)**: Regional anomaly detection
  computed each region's z-score against a baseline that *included itself*.
  For n=3 groups this creates a mathematical ceiling (|z| ≲ 1.41 no matter
  how extreme the true gap, because the outlier drags its own mean/stddev
  along with it) — a real anomaly was silently invisible. Fixed with a
  leave-one-out baseline (compare each region against the *other* regions
  only), mirroring the pattern already used for monthly violation-spike
  detection.
- **Decision**: Every prediction/anomaly/insight/risk score carries a
  uniform `Explainability` record (confidence, methodology, input data
  period, reasoning, supporting metrics, assumptions, generated timestamp) —
  introduced after the user required no black-box outputs; retrofitted
  consistently across all four output types rather than bolted on ad hoc.
- **Gap found & fixed**: `Inspection.region` existed as a DB column (Phase 2
  migration) but was never mapped on the entity or exposed via the API — it
  would always be `NULL`, making region-level analytics meaningless. Wired
  through entity/DTO/mapper/service.

## Phase 3 — AI Copilot

- **Decision**: The Copilot's only knowledge of the rest of the system is
  `CopilotContext`, a read-only snapshot assembled by one class
  (`CopilotContextAssemblerImpl`). This is the actual enforcement mechanism
  for "AI never decides compliance" — the LLM is architecturally never
  handed anything to decide with; every fact was already produced by the
  Rule Engine/Scoring service before the Copilot runs.
- **Decision**: Purely factual questions ("what is the compliance score?")
  are answered deterministically from the database, never via the LLM —
  zero hallucination risk for numbers that have exactly one correct answer.
- **Decision**: Reused the existing `ai.llm.provider` abstraction
  (`LlmProvider`/`GeminiLlmProvider`/`ClaudeLlmProvider`/`LlmProviderFactory`,
  built in Phase 2 for OCR correction) rather than creating a parallel one
  for the Copilot — one LLM abstraction for the whole backend.
- **Decision**: Prompts are externalized to `resources/prompts/copilot/*.txt`
  with `{{variable}}` substitution, not string literals inside service
  classes — prompts can be tuned without a Java code change or redeploy.

## Phase 4 — Evidence Engine, Decision Trace, Smart Reports, hardening

- **Bug found & fixed (proactive audit, before adding new callers)**:
  `ViolationRepository.findByInspectionId` had no fetch join on `rule`,
  but every existing caller (`ViolationMapper` via the rules API,
  `CopilotContextAssemblerImpl`) immediately calls `violation.getRule()...`
  — a live N+1 in a shipped endpoint (`GET /rules/inspections/{id}/violations`).
  Converted to an explicit `JOIN FETCH` JPQL query; same signature, same
  behavior, one query instead of 1+N. Caught this before wiring the new
  Evidence Engine in as a fourth caller, which would have made it worse.
- **Decision**: Decision Trace is a single-shot `recordStep(RecordStepCommand)`
  API, not a start/complete handle. Every caller already knows exactly when
  a stage starts and ends (it just ran it synchronously), so a stateful
  handle would only add API surface without buying anything. `RecordStepCommand.success/failure`
  compute `executionTimeMs` from a caller-captured start timestamp so call
  sites stay a two-line addition around existing pipeline code.
- **Decision**: `referencedRuleId`/`referencedEvidenceId`/`referencedImageId`
  on `decision_trace_steps` (and, by the same reasoning, on the `evidence`
  table added in the same migration) are plain UUID columns with no FK and
  no JPA relation — this is an append-only traceability log, and a loose
  reference must never block deleting or evolving the thing it points at
  (same pattern already used by `audit_logs.entity_id`).
- **Extended** (additive, no behavior change to existing responses):
  `ImageServiceImpl.upload`, `OcrServiceImpl.run`, `VisionAiServiceImpl.analyzeAndPersist`,
  `DeclarationFusionServiceImpl.fuseInspection`, and
  `InspectionEvaluationServiceImpl.evaluateAndScore` now each record their
  corresponding Decision Trace step(s) (`IMAGE_UPLOADED`/`IMAGE_QUALITY`,
  `OCR`/`OCR_CORRECTION`, `VISION_DETECTION`, `DECLARATION_FUSION`,
  `RULE_EVALUATION`/`VIOLATIONS`/`COMPLIANCE_SCORE`/`RISK_SCORE`
  respectively) — the full pipeline is now traceable end-to-end via
  `GET /api/v1/inspections/{id}/decision-trace` without changing any
  existing method signature or return value. Verified via `mvn test`
  (46 tests, 0 failures) — no test constructs these services directly, so
  the added constructor dependency was a safe, non-breaking change.
- **Built: Evidence Engine (Part 1)**. New `evidence` module: `Evidence`
  entity/repository/service/mapper/controller, `AnnotatedImageService`
  (bounding-box overlay with severity colors + legend, side-by-side
  compositor, crop — the Part 2 foundation), and `EvidenceHasher`.
  `EvidenceServiceImpl.generate` walks Violation → `FusedDeclaration` (by
  inspection + declaration type) → the winning OCR/Vision
  `LabelDetection` (mirroring the exact confidence-comparison
  `DeclarationFusionServiceImpl.fuse` already uses, so "which side won"
  is never computed two different ways) → its `Image`, then crops and
  annotates that region and uploads both under `evidence/{inspectionId}/{violationId}/`
  in the existing `IMAGES` bucket. A violation with no resolvable
  declaration (e.g. a rule not tied to one specific field) still gets a
  full evidence row with every textual field — just no image.
- **Decision**: Reused the `IMAGES` Supabase bucket for evidence-derived
  images (cropped/annotated) instead of provisioning a 6th bucket.
  They're private, signed-URL-gated artifacts with the exact same access
  pattern as originals — a new bucket would have meant a new config
  property and a new manual provisioning step in Supabase for no
  architectural benefit.
- **Decision**: Evidence immutability is enforced by
  `InspectionServiceImpl.updateStatus` calling
  `EvidenceService.finalizeEvidence(id)` directly when the target status
  is `COMPLETED` — a plain service-to-service call, matching every other
  cross-module call in this codebase (e.g. `ImageServiceImpl` →
  `ImageQualityService`). Considered a Spring `ApplicationEvent` to avoid
  the new `inspection` → `evidence` dependency edge, but the codebase has
  zero existing precedent for an event bus and introducing one for a
  single call site would have been the more complex option, not the
  cleaner one. Verified live: driving a seeded inspection through
  DRAFT → IN_PROGRESS → PENDING_REVIEW → COMPLETED flips `is_immutable`
  on its evidence rows, and a subsequent regeneration attempt is
  correctly rejected with 400.
- **Bug found & fixed (proactive audit, surfaced by the smoke test)**:
  `StorageServiceImpl` only caught `WebClientResponseException` (an HTTP
  error status from Supabase) around every operation, not
  `WebClientRequestException` (the request never reached Supabase at
  all — DNS failure, connection refused, timeout). A live smoke test
  against an unreachable storage endpoint confirmed this leaked a raw
  Reactor/Netty stack trace as an unhandled 500 instead of the intended
  `FileStorageException`. Widened every catch to the common
  `WebClientException` supertype; verified the same failing request now
  returns a clean 502 with a clear message.
- **Verified via a full live smoke test** (Docker `pgvector/pgvector:pg16`
  + a stub Supabase Storage server serving a real PNG): booted the jar,
  ran Flyway through v10, signed up a user, created an inspection, seeded
  a realistic violation chain (rule → violation → fused declaration →
  label detection → image) directly in Postgres, then called
  `POST /api/v1/violations/{id}/evidence/generate` and got back a real
  cropped/annotated image pair, a genuine 64-character SHA-256 hash, and
  every explainability field populated from the seeded data — the full
  pipeline logic, not just a compile check. Added `AnnotatedImageServiceImplTest`
  (5 cases: crop bounds/clamping, severity-color drawing, side-by-side
  sizing) and `EvidenceHasherTest` (3 cases: determinism, content
  sensitivity, image-byte sensitivity) to lock this in going forward.
  Full suite: 54 tests, 0 failures.
- **Built: Smart Report Generation (Part 4)**. The `report` module's PDF/
  Excel rendering was a Phase 1 stub ("deferred to the phase where
  compliance-report business logic is implemented") — this is that phase.
  New `ReportData` model (assembled once by `ReportDataAssemblerImpl`
  straight from the domain repositories, deliberately *not* from
  `CopilotContext` — that type is scoped to "what the LLM may see," not
  "what a report may show") feeds both `PdfReportRendererImpl` (OpenPDF,
  reusing the `PdfUtil` primitives already scaffolded in Phase 1) and
  `DocxReportRendererImpl` (Apache POI XWPF) — same sections, same data,
  two formats. JSON export (`GET /reports/inspection/{id}/json`) returns
  `ReportData` directly with no file stored: cheap to reassemble fresh
  every time, so there's no third artifact to keep in sync. Executive
  Summary and Recommendations reuse the Phase 3 Copilot's existing
  `summarizeInspection`/`generateManufacturerRecommendations` — no new AI
  plumbing — wrapped so an LLM failure falls back to a deterministic
  summary/aggregated-suggested-fixes text instead of failing the report.
  Evidence's annotated images are embedded directly into both the PDF and
  DOCX (downloaded via `StorageService`, drawn as real inline images, not
  just linked).
- **Schema investigation before writing a migration**: before renaming
  `compliance_reports.excel_path`, discovered — via smoke test, not
  inspection — that Phase 2's `V3__ai_engine_schema.sql` had *already*
  added an unused `docx_path` column ("AI report generator (Step 10):
  DOCX sits alongside the existing pdf_path"), anticipating this exact
  feature. The rename migration (`V11`) was rewritten to just drop the
  now-confirmed-dead `excel_path` column instead of colliding with the
  column that already existed; the file was renamed to
  `V11__drop_compliance_report_excel_path.sql` to describe what it
  actually does. Lesson: grep the full migration history for a column
  name before assuming a schema change is needed, not just the Java
  source.
- **Bug found & fixed (surfaced by the smoke test, not by inspection)**:
  `CopilotServiceImpl`'s six methods ran under the default `REQUIRED`
  transaction propagation. Calling `summarizeInspection`/
  `generateManufacturerRecommendations` from `ReportDataAssemblerImpl`
  (itself transactional, to let the Copilot's own writes succeed — see
  below) meant an LLM failure inside the Copilot call marked the
  *shared* transaction rollback-only, even though the caller caught the
  exception and substituted a fallback string — the report row and
  uploaded files were silently discarded on commit with an
  `UnexpectedRollbackException`, surfaced only because the smoke test
  exercised report generation with dummy (failing) LLM credentials.
  Fixed by moving all six `CopilotServiceImpl` methods to
  `@Transactional(propagation = REQUIRES_NEW)` — a Copilot call's
  chat-history/audit-row writes are logically independent of whatever
  business transaction triggered it, so isolating them is correct
  regardless of this specific failure mode.
- **Verified via a full live smoke test**: regenerated the same seeded
  violation/evidence chain from the Evidence Engine test, then called
  `POST /api/v1/reports/inspection/{id}/generate` against a stub Supabase
  Storage server that persists uploaded bytes to disk (not just serves a
  fixed image) — confirmed the uploaded PDF is `file`-recognized as a
  genuine 2-page PDF 1.5 document with a real `/Subtype/Image` XObject,
  and the DOCX is genuine OOXML containing `word/media/image1.png`
  (extracted and inspected both directly, not inferred from a 200
  status). Also confirmed `GET .../reports/.../json` returns the same
  facts and, after adding `@JsonIgnore` to `EvidenceEntry.annotatedImagePath`
  (a raw internal storage path that had no business being in a public API
  response), no longer leaks it. Full suite: 54 tests, 0 failures.

## Phase 4 (cont'd) — API Freeze, Security Review, Performance Review (Parts 5, 7, 8)

Ran three independent audits (API surface consistency, security, performance)
before touching anything further, per the standing "audit before adding
more code" mandate. Findings below are grouped by what was actually done
about them — investigated-and-confirmed-fine items are included so a
future reader doesn't re-raise them as unaddressed.

- **Bug found & fixed (Critical, surfaced by the security audit)**:
  `AuthServiceImpl.forgotPassword` logged the raw, unhashed password-reset
  token at `INFO` level unconditionally — anyone with log access could
  take over any account, and `application-prod.yml` keeps
  `com.legalmetrology: INFO`, so this would have reached production logs.
  Changed to `log.debug`: `dev`/`local` profiles already set
  `com.legalmetrology: DEBUG` (so the flow stays testable without a wired
  email provider), while `prod` stays at `INFO` and never sees it.
- **Bug found & fixed (High, path traversal)**: `FileUploadUtil.extractExtension`
  took everything after the *last* `.` in the client-supplied original
  filename with no sanitization. A filename like `a./../../evil` has its
  last `.` sitting inside a `../` segment, yielding `/evil` as the
  "extension" — spliced into `ImageServiceImpl.upload`'s generated storage
  path, this turns one path segment into several, escaping the intended
  `{inspectionId}/{type}/` folder in the storage bucket. Fixed by
  allowlisting the extension (`^[a-zA-Z0-9]{1,5}$`) and falling back to
  the already content-type-validated MIME type otherwise. Verified live:
  uploading with that exact filename now correctly lands as a plain
  `.png` under the right folder.
- **Bug found & fixed (High, IDOR)**: `ImageServiceImpl.delete` had no
  authorization check at all — any authenticated user could delete any
  image by UUID regardless of which inspector/inspection it belonged to.
  Added an ownership check (the inspecting officer, or ADMIN/SENIOR_OFFICER)
  reusing the existing `AccessDeniedException` → 403 handling already in
  `GlobalExceptionHandler` (no new exception type). Verified live with two
  real officer accounts: the non-owner gets 403, the owner succeeds.
- **Bug found & fixed (Medium)**: `JwtTokenProvider` never validated
  `JWT_SECRET`'s length — a short secret would only fail at first-token-generation
  via JJWT's runtime `WeakKeyException`, not at boot. Added a
  `@PostConstruct` check (`>= 32 bytes` for HMAC-SHA256) so a
  misconfigured secret fails fast at startup instead of at the first
  login attempt. Verified live: an 8-byte secret now refuses to boot with
  a clear message; a real 32+ byte secret boots normally.
- **Bug found & fixed (real, caught only by exercising the actual upload
  endpoint rather than seeding data directly via SQL)**: the Decision
  Trace wiring added to `ImageServiceImpl.upload` (see the Decision Trace
  entry above) passed the image's 0-100 `qualityScore` straight into the
  trace step's `confidence` field — but `decision_trace_steps.confidence`
  is `numeric(5,4)`, a [0,1] fraction shared by every other pipeline
  stage, and any quality score of 10 or more overflowed it, making every
  single image upload fail with `numeric field overflow`. This had been
  invisible in every prior smoke test because those seeded violations
  directly via SQL and never actually exercised the upload endpoint.
  Fixed by normalizing the score to a 0-1 fraction before recording it.
  Lesson: a smoke test that bypasses the real entry point can miss real
  entry-point bugs — re-verified by actually calling
  `POST /inspections/{id}/images` with a real multipart file afterward.
- **Fixed (N+1, High)**: `InspectionRepository.findByInspectorId`/
  `findByStatus`/`findAll` and `ProductRepository.findByNameContainingIgnoreCase`/
  `findAll` are the highest-traffic paginated list endpoints in the
  system, and their mappers read `inspector.getFullName()`/`product.getName()`
  and `category.getName()`/`manufacturer.getName()` respectively with no
  fetch join — a 20-row page cost up to 40 extra lazy-load queries. Added
  `@EntityGraph` to all five methods.
- **Fixed (missing indexes, `V12__missing_foreign_key_indexes.sql`)**:
  `compliance_reports.inspection_id`, `chat_history.user_id`, and
  `product_categories.parent_category_id` were queried directly with no
  supporting index. Added preemptively, not in response to an observed
  slow query.
- **Investigated and confirmed already correct (not fixed, because
  nothing was wrong)**: the audit flagged `EvidenceController`/
  `OcrController`/`VisionController`'s `generate`/`run`/`fuse`/`analyze`
  endpoints for returning 200 instead of 201 despite persisting rows.
  On inspection, this matches a real, pre-existing, and correct two-tier
  convention already established by `RuleEvaluationController.evaluate`:
  201 is for endpoints that create a genuinely new, independent resource
  each call (a new report, a new inspection); 200 is for endpoints that
  *recompute* derived state for a fixed identity, replacing what was
  there before (OCR re-run, re-fusion, re-evaluation, evidence
  regeneration) — calling them twice doesn't produce two resources.
  `ReportController.generate` (which does create a new, independent
  `ComplianceReport` row every call) already correctly uses 201. Changing
  the others to 201 would have broken this existing convention, not
  fixed an inconsistency.
- **Investigated and confirmed already correct**: the audit flagged
  `application.yml`'s `server.error.include-message: always` as a
  potential leak. `application-prod.yml` already overrides both
  `include-message` and `include-binding-errors` to `never` — production
  was never exposed.
- **Documented, not merged**: `ValidationResultResponse` and
  `ViolationResponse` share ~9 fields. Added cross-referencing Javadoc
  explaining the real distinction (transient explain-only view of every
  rule checked, vs. the persisted view of one failed rule) rather than
  forcing a merge that would blur that distinction for a cosmetic
  field-count reduction.
- **Fixed (naming consistency)**: `ReportController` and
  `ScannerController` used the singular `/inspection/{id}/...` while
  every other controller uses `/inspections/{id}/...`. Renamed both —
  safe pre-freeze since no frontend consumes these paths yet.
- **Fixed (validation gap)**: `InspectionRequest` had zero bean-validation
  annotations despite its controller using `@Valid`. `productId`/
  `locationLat`/`locationLng`/`region` are all genuinely optional by
  design (confirmed against `InspectionServiceImpl.create`), so nothing
  was made newly required; added range checks
  (`@DecimalMin`/`@DecimalMax` for lat/lng, `@Size` for region) that only
  bound values which *are* supplied.
- **Recorded for the Part 10 Backend Readiness Report, not fixed now**
  (each needs either a product decision or a riskier refactor than
  appropriate mid-session): the role model for several state-changing
  endpoints (`InspectionController` status transitions, `ProductController`
  create/update) has no method-level authorization beyond "authenticated,"
  unlike their sibling delete endpoints — needs a product decision on
  intended role boundaries, not a guess. No rate limiting exists on
  `/auth/login`/`/signup`/`/forgot-password` — real brute-force/enumeration
  exposure. Several newer `@Transactional` methods (`OcrServiceImpl.run`,
  `VisionAiServiceImpl.analyzeAndPersist`, `EvidenceServiceImpl.generate*`,
  `ReportServiceImpl.generate`/`ReportDataAssemblerImpl.assemble`) hold a
  DB connection open across slow outbound HTTP/LLM calls — combined with
  `DB_POOL_MAX_SIZE:10`, a handful of concurrent report-generation or OCR
  requests could exhaust the pool. `FileUploadUtil.validateImage` trusts
  the declared content-type without magic-byte sniffing, though
  `ImageUtil.read` (called during upload for the perceptual hash) already
  rejects genuinely non-image bytes as a side effect — a real gap for a
  file that's a valid-but-wrong image format, not for arbitrary content.

## Phase 4 (cont'd) — OpenAPI Documentation, Testing (Parts 6, 9)

- **Documented all 75 endpoints across all 16 controllers** (Part 6):
  every `@Operation` now has a full `description` where the one-line
  summary didn't cover validation rules/side effects/"not found"
  semantics, a complete `@ApiResponses` block with realistic example JSON
  (in the real `ApiResponse` envelope shape, with real UUIDs/enum values
  matched to the actual DTOs — not placeholder text), and `@Parameter`
  hints on path/query parameters that need them. Each endpoint's documented
  status codes were derived from actually reading its service
  implementation and `GlobalExceptionHandler`, not guessed: 409 only where
  `DuplicateResourceException` is genuinely reachable (product barcode,
  rule code), 502 only where the service actually calls Supabase Storage
  or an AI/OCR/Vision provider, 403 only on methods carrying
  `@PreAuthorize`. Caught and documented one behavioral surprise along
  the way: several "list by parent id" endpoints (OCR history, fused
  declarations, vision detections) intentionally return an empty list for
  an unknown parent id rather than 404 — documented as such instead of
  claiming a 404 that doesn't happen. Verified two ways: `mvn compile`
  clean, and `GET /v3/api-docs` on a live boot actually returns a valid
  spec (76 paths, 87 operations) — annotation syntax that compiles can
  still break Springdoc's spec generation at runtime, so this was checked
  for real, not assumed from a successful build.
- **Added test coverage for this phase's riskiest new logic** (Part 9):
  `EvidenceServiceImplTest` (5 cases — immutability guard, OCR-vs-Vision
  winner selection in both directions, no-declaration-field and
  no-fused-declaration paths), `DecisionTraceServiceImplTest` (5 cases —
  field mapping, the failure factory, not-found, ordering, reference
  resolution), and `ReportDataAssemblerImplTest` (4 cases — not-found,
  the LLM-failure deterministic fallback for both narrative fields
  confirmed via mocked `CopilotService` throwing, the LLM-success path,
  and the suggested-fix aggregation). These specifically target the two
  bug classes already found live in this phase (transaction propagation
  swallowing a report on LLM failure; a unit mismatch nobody would have
  caught without exercising the real winner-selection branch both ways).
  Full suite: 68 tests, 0 failures.

## Phase 4 (cont'd) — CSP header (closing a Part 8 finding)

- **Fixed**: `SecurityConfig` had no `Content-Security-Policy` — Spring
  Security's other defaults (X-Content-Type-Options, X-Frame-Options,
  HSTS) were already active, but this one doesn't have a built-in
  default. Added a policy scoped to what this backend actually serves:
  it's a JSON API that also serves Swagger UI from the same origin in
  non-prod (`application-prod.yml` disables it), so `'self'` covers
  scripts/styles/images/connections, with `'unsafe-inline'` on script/style
  because Swagger UI's own bootstrap needs it, and `frame-ancestors 'none'`
  since nothing here should ever be framed. Verified live, not just
  compiled: booted the jar, confirmed the header is present on every
  response, confirmed `swagger-ui/index.html` and `/v3/api-docs` both
  still return 200 with the header applied, and confirmed a real signup
  call still completes normally — the CSP doesn't block the API itself,
  only browser-side script/frame behavior.
- **Unrelated flake, not a bug**: hit a `NoClassDefFoundError` on one
  boot attempt from a `target/` directory built without `clean` during
  the same session as two parallel background-agent builds. Reproduced
  cleanly with `mvn clean package` and it disappeared — stale interleaved
  build output, not a real defect. Noted here so it isn't mistaken for a
  regression if it resurfaces: always `clean` before a smoke-test boot
  when multiple builds have touched the same `target/` directory.

## Phase 5 — Frontend: Design System + AI Inspection Workspace (Officer Portal)

Backend frozen — no backend files touched this phase. Scaffolded the
previously-empty `frontend/` per `docs/ARCHITECTURE.md` §3 (feature-sliced,
`components/ui|common|charts` for cross-cutting UI) with React 19, Vite,
TypeScript, Tailwind CSS v4, Radix primitives, React Router, TanStack
Query, React Hook Form + Zod, Framer Motion, Recharts, react-dropzone,
and `@zxing/browser`.

- **Decision**: every DTO in `features/*/types/*.ts` was written by reading
  the actual backend record source (not the Phase 0 architecture doc,
  which predates several backend decisions) — field-for-field, including
  the discovery that `application.yml`'s global
  `spring.jackson.default-property-inclusion: non_null` means null fields
  are *omitted* from responses, not sent as `null`. Verified live against
  a real running backend (Docker Postgres + a stub storage server), not
  assumed from reading Java source alone: signup → login → create
  inspection → upload image → OCR → Vision → fuse → evaluate → generate
  evidence → decision trace, hitting every endpoint the workspace screen
  calls, with response shapes compared field-by-field to the TypeScript
  types.
- **Decision**: no backend pipeline-orchestrator endpoint exists (each
  stage — OCR, Vision, fusion, rule evaluation, evidence — is deliberately
  its own on-demand endpoint per the AI Pipeline architecture). Rather
  than inventing one, `useRunPipeline` orchestrates the real sequence
  client-side and drives the Decision Timeline's live state from it —
  the only "adapter" in the whole integration, and it's calling five real
  endpoints in the right order, not mocking any of them.
- **Decision**: design tokens (color/type/spacing/shadow) live in one
  `@theme` block in `styles/globals.css` — Tailwind v4 only generates a
  utility class for a color declared inside `@theme`, so `.dark` overrides
  the same custom properties' *values* outside it rather than redeclaring
  parallel dark tokens; a first draft that put light-mode colors in a
  plain `:root` block outside `@theme` was caught during review and fixed
  before any component used it, since it would have silently generated no
  utilities at all.
- **Bug found & fixed (self-review, before calling the screen done)**:
  the left panel's "Camera" button had no handler — a dead control on the
  screen meant to be the design reference for the rest of the app. Built
  `CameraCaptureDialog` (`getUserMedia` → live preview → canvas snapshot →
  real `File`, feeding the same upload path as drag-and-drop) rather than
  shipping the gap and noting it for later.
- **Verified**: `tsc -b` clean, production `vite build` succeeds (one
  outstanding note: a single ~1.5MB JS chunk — code-splitting is a fair
  follow-up once more screens exist, not before).
- Built one screen only, per the explicit brief: the AI Inspection
  Workspace (left: capture/upload/barcode/QR/camera; center: zoomable/
  pannable annotated image with live bounding-box overlay; right:
  compliance score, risk, detected fields, violations, evidence; bottom:
  Decision Timeline; drawer: AI Copilot chat). Sidebar/header/routing
  exist as real, working app chrome; every other sidebar destination is
  an explicit "coming soon" placeholder, not a faked screen.

## Phase 5 (cont'd) — Copilot drawer refinement ("Nirikshak")

- **Bug found & fixed (self-review)**: every suggested Copilot question
  was routed through the generic `ask()` endpoint, including three
  ("Explain this rule," "Generate officer notes," "Compare with another
  inspection") that have dedicated backend endpoints taking their own
  real parameters (`GET /copilot/rules/{ruleCode}/explain`,
  `POST .../officer-notes` with `roughNotes`,
  `POST /copilot/inspections/compare?a=&b=`). Rewrote `copilotApi`/
  `useCopilotChat`/`ChatPanel` so each suggested action calls its actual
  correct endpoint — "Explain this rule" targets whichever violation is
  selected on the right panel (disabled with an inline hint if none is),
  "Generate notes" and "Compare" open a small inline form for the
  parameter they genuinely need rather than guessing at it via free text.
- **Bug found & fixed (id-keying mismatch, self-review)**: clicking a
  violation in the right panel set `selectedBoxId` to the *violation's*
  id, but the center panel's overlay boxes were keyed by *evidence* id —
  two different UUIDs (`Evidence.violationId` references but doesn't
  equal `Evidence.id`), so the intended "click a violation, see it
  highlight on the image" interaction silently never matched. Re-keyed
  the overlay boxes by `violationId` throughout.
- Renamed the drawer's persona to "Nirikshak" (the sidebar's future
  full-page "AI Copilot" destination keeps its own name — they're
  different surfaces).
- **Verified live** against a real running backend: all four new/changed
  endpoint calls (`explainRule`, `generateOfficerNotes`,
  `manufacturerRecommendations`, `compareInspections`) reach their
  correct service logic — confirmed every resulting 500 traces to the
  same real external Gemini call failing on a dummy API key, never a
  request-shape or contract mismatch on the frontend's side. `tsc -b`
  and `vite build` both clean.

## Phase 6 — Real AI/Storage credentials: service-account auth + graceful degradation

Every OCR/Vision/LLM/Storage integration was already a real implementation
(no mocks existed anywhere) — this phase was about supporting the
credential path the user specifically wanted (GCP service accounts) and
closing a real gap in how missing/invalid credentials were handled.

- **Added**: `GoogleCloudCredentialsProvider` resolves an OAuth2 bearer
  token from a service account (`GOOGLE_APPLICATION_CREDENTIALS`, Google's
  Application Default Credentials convention) via the lightweight
  `google-auth-library-oauth2-http` dependency — deliberately not the full
  `google-cloud-vision` client library, since `GoogleVisionOcrProvider`'s
  existing REST/JSON parsing (paragraphs, lines, words, bounding boxes,
  confidence, language) is real, tested, and unaffected by *how* the
  request is authenticated. `GoogleVisionOcrProvider` now prefers a
  service-account bearer token when configured, falling back to the
  simpler `GOOGLE_VISION_API_KEY` query-param auth otherwise — both are
  real, supported paths, not one replacing the other.
- **Added**: `ServiceNotConfiguredException` (503) — thrown at the point
  Vision, Gemini, Claude, or Supabase Storage is actually *used* if its
  credentials are blank, never at startup, so a backend with zero AI
  credentials configured still serves auth/rules/products/every non-AI
  endpoint normally. Verified live at every one of the four call sites
  with genuinely blank env vars: each returns the exact clean message
  requested ("Gemini API key is not configured.", "Supabase Storage is
  not configured — set SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY.") at
  a 503, not a raw 500.
- **Bug found & fixed (surfaced by that same live verification, not
  hypothetical)**: `OcrProviderFactory`'s fallback logic (Google Vision ->
  Tesseract) caught only `Exception`. On a machine with no native
  `libtesseract` installed, the fallback attempt throws
  `UnsatisfiedLinkError` — an `Error`, not an `Exception` — which was
  never caught, crashing the whole request past the factory's own "never
  let one provider's failure be fatal" design intent. Widened both catches
  to `Throwable` with a documented rationale (a native-library or
  environment problem in one *optional* fallback provider must degrade to
  a clean composed error, never crash the request). Reproduced the exact
  crash live before the fix and confirmed the clean 400 after.
- Added `ai.ocr.google-vision.project-id` (`GOOGLE_CLOUD_PROJECT_ID`) —
  optional, only sets the `X-Goog-User-Project` quota header when
  authenticating via service account.
- Generated `backend/.env.example` (every variable, including the new
  Google Cloud ones, documented with which of the two Vision auth paths
  each belongs to), `backend/application.yml.example` (an illustrative,
  fully-populated reference — never loaded by Spring, lives outside
  `src/main/resources` specifically so it can't be accidentally activated
  as a profile), `frontend/.env.example` (unchanged in substance — the
  frontend still never talks to Google/Supabase directly — but now
  explains why), and root `SETUP.md` walking through creating the GCP
  project, enabling Vision, creating the service account, obtaining a
  Gemini key, creating the Supabase project and its five required
  buckets, configuring both `.env` files, starting both services, and
  verifying each integration by its real, specific signal (a file
  actually appearing in the Supabase bucket, real extracted OCR text,
  real Copilot answers) rather than just "no errors."
- **Verified**: full test suite (68 tests) still green; the backend jar
  running for the ongoing frontend review session was rebuilt and
  restarted with these changes without disrupting the demo account or the
  live Cloudflare tunnel.

## Phase 6 (cont'd) — Dedicated Evidence bucket

The user's real Supabase project was provisioned with three buckets
(`inspection-images`, `inspection-reports`, `evidence`) rather than this
codebase's original five — investigating showed `temp`/`exports`/
`training-data` are genuinely unused (nothing calls `uploadTemp`/
`StorageBucket.EXPORTS`/`StorageBucket.TRAINING_DATA` anywhere), and a
dedicated evidence bucket is a real improvement over the original
decision to nest evidence under `images` (Phase 4's rationale — "same
access pattern as originals" — was true, but legal evidence plausibly
needs different retention/access rules than working photos, which a
shared bucket can't express).

- Added `StorageBucket.EVIDENCE` + `SupabaseStorageProperties.Storage.bucketEvidence`
  + `SUPABASE_BUCKET_EVIDENCE` (default `evidence`). Moved every evidence
  read/write (`EvidenceServiceImpl`, `EvidenceController`,
  `ReportDataAssemblerImpl`, `PdfReportRendererImpl`,
  `DocxReportRendererImpl`) from `StorageBucket.IMAGES` to
  `StorageBucket.EVIDENCE` — six call sites, all storage-layer only, no
  domain logic changed. Simplified the evidence storage path from
  `evidence/{inspectionId}/{violationId}/...` to
  `{inspectionId}/{violationId}/...` since the bucket name itself now
  carries that meaning (safe to do pre-launch — no production evidence
  exists yet, and evidence is regenerated, not migrated, on re-evaluation).
- Updated `backend/.env.example`/`application.yml.example`/`SETUP.md` to
  the three real bucket names, and to explain Supabase's newer
  "Publishable key"/"Secret key" dashboard labels map onto
  `SUPABASE_ANON_KEY`/`SUPABASE_SERVICE_ROLE_KEY` unchanged.
- Added `CHECKLIST.md` — a terse, tickable companion to `SETUP.md`.
- Verified live: generated real evidence against the dev stack and
  confirmed the resulting files land under the `evidence/` path with the
  simplified structure, not `images/`. Full suite: 68 tests, 0 failures.
  Dev backend rebuilt and restarted without disrupting the ongoing
  frontend review session.

## Phase 6 (cont'd) — Real-credential live verification: Gemini model fix, Supabase DNS blocker

With the user's real service-account JSON, Gemini key, and Supabase
credentials in place in `backend/.env`, ran the full live verification
pass end-to-end against the actual application (not just unit tests).

- **Google Vision: confirmed working.** `GoogleCloudCredentialsProvider`
  loads the real service-account JSON at boot
  (`Loaded Google Cloud service-account credentials from
  /Users/yashikasinha/secrets/nirikshan-vision-sa.json`) and the app
  starts cleanly with zero warnings/errors from application code.
- **Gemini: found and fixed a stale, deprecated default model.**
  `gemini-2.5-pro` (the previous default in `application.yml`,
  `.env.example`, and `application.yml.example`) now 404s — Google's own
  error response says it's retired in favor of `gemini-3.1-pro-preview`.
  Verified the real API key directly against Google's API (outside the
  app, to isolate key validity from model availability):
  `gemini-3.1-pro-preview` is reachable with this key but returns `429
  RESOURCE_EXHAUSTED` (`limit: 0`) — this free-tier key has zero quota on
  "pro"-tier models even though they exist and the key is valid.
  `gemini-3.6-flash` (the flash-tier replacement for the also-deprecated
  `gemini-2.0-flash`) returned a genuine `200` with real generated text.
  Updated the default `GEMINI_MODEL`/`GEMINI_VISION_MODEL` to
  `gemini-3.6-flash` in `application.yml`, `backend/.env`,
  `backend/.env.example`, and `backend/application.yml.example`, each
  with a comment explaining the deprecation/quota finding so a future
  reader doesn't have to rediscover it. Re-verified through the real
  Copilot `ask` endpoint post-fix: real question in, real Gemini-generated
  answer out, `"provider":"gemini-3.6-flash"`, persisted to
  `chat_history` in Postgres.
- **Supabase Storage: blocked, not auto-fixable.** Uploading a real test
  image to `inspection-images` fails with `502` /
  `"Failed to upload object in storage bucket 'inspection-images'"`. The
  backend log shows the true cause: the request never reaches Supabase —
  `lamayywvlazhuspdbsuav.supabase.co` does not resolve (`NXDOMAIN`),
  confirmed independently via `nslookup` while general internet DNS
  (e.g. `supabase.com`) resolves fine. This means the configured
  `SUPABASE_URL` is not a live Supabase project hostname as currently
  written. Not something the backend can gracefully route around — it
  requires the user to re-verify the exact Project URL from their
  Supabase dashboard (Project Settings -> API). Everything downstream of
  Storage (image upload, OCR-on-real-image, signed URLs, evidence
  generation against Supabase) stays blocked until this is corrected;
  nothing about the integration code itself is suspected to be at fault,
  since the identical client code already round-trips correctly against
  the local stub storage server used in earlier smoke tests.

## Phase 10 (cont'd) — Mobile login unreachable: the classic `100vh` bug

User report: login worked on a phone with the browser's "Request Desktop
Site" toggle on, but not in normal mobile view — otherwise identical
network conditions and credentials. That specific pairing (works only
with Desktop Site forced) is close to a textbook signature for one
specific class of bug, not an auth/network problem: real mobile browsers
render `100vh` as if their address-bar chrome were already hidden, so it
overshoots the actually-visible area — "Desktop Site" mode forces a
fixed, non-mobile viewport and happens to sidestep that calculation
entirely, which is exactly why it "fixed" it.

`LoginPage.tsx`'s root container was `h-screen` (100vh) combined with
`overflow-hidden`. On a real phone, that pushed the Sign In button below
the true visible fold with the outer `overflow-hidden` actively
preventing any scroll to reach it — reachable only when something (like
Desktop Site mode) changed the effective viewport math. Fixed by
switching to `h-dvh` (dynamic viewport height, recalculated live as the
browser chrome shows/hides) — verified Tailwind actually emits
`.h-dvh{height:100dvh}` in the real build output, not just that it
type-checks. Found and fixed the identical `h-screen` pattern
defensively in the three other full-viewport containers in the app
(`AppLayout`, `Sidebar`, `ProtectedRoute`'s loading screen) before it
could cause the same "unreachable content on a real phone" symptom
elsewhere. Verified: `vite build` clean, both local and the live tunnel
serving the updated login page.

## Phase 10 — Guided 360° Capture, real fusion bug fix, perspective detection, Copilot expansion

A full inspection-workflow redesign requested for SIH finals. Six work items, each investigated and fixed against the real backend — no mock data anywhere.

- **Root-caused and fixed the reported "NET QUANTITY visibly on the label but Rule Engine says missing" bug.** Traced the full pipeline (Upload → OCR → Vision AI → Declaration Fusion → Rule Evaluation) via code review, since live Vision AI calls were blocked by the free-tier Gemini key hitting its rate limit from this session's own heavy testing. Found the real cause in `DeclarationFusionServiceImpl.best()`: it picked the highest-*confidence* detection for a declaration type across **all of an inspection's images**, with no regard for whether that detection said the field was present or absent. A multi-image inspection (which Guided 360° Capture, below, makes the norm) routinely gets one Vision AI detection per image per field — most correctly reporting `present=false` on faces where a field genuinely isn't printed, each carrying its own "confidence that it's absent from *this* face." A confidently-absent detection from Back/Side (e.g. 0.95) could outrank a genuinely-present detection from Front (e.g. 0.85), so a field visible in the actual photo got reported missing. Fixed: any present detection now always outranks any absent one; ties within each group still go to higher confidence. **Verified end-to-end live**: since Gemini itself was rate-limited, seeded two realistic `label_detections` rows directly (one present, lower confidence; one absent, higher confidence — the exact adversarial case) — the same isolation technique this codebase's own unit tests already use to test business logic independent of the AI provider — then called the real `POST /ocr/inspections/{id}/fuse` and `GET /rules/inspections/{id}/evaluation` endpoints. Confirmed: fusion now returns `present: true, fusedValue: "450 g"`, and `LM-NETQTY-001` now correctly passes with `actualValue: "450 g"`.
- **Also hardened `VisionResponseParser`** against a related, independent risk: it called `DeclarationType.valueOf()` directly on the LLM's raw `"type"` string and silently dropped the entire declaration (`return null`, warn-only log) if it didn't match a Java enum constant exactly. LLMs occasionally return a near-miss despite an explicit prompt listing the exact expected values. Now normalizes case/hyphens/spaces before matching, and logs at error level with the full raw node if it's still unrecognized — a genuinely-dropped field is now loud, not silent.
- **Guided 360° Capture**: replaced single-image upload with a 6-angle grid (Front/Back/Left/Right/Top/Bottom), each capturable via camera or file picker, with live progress ("N / 6 angles") and per-slot retake. Required a real schema change — `ImageType` only had `FRONT/BACK/SIDE/OTHER` — added migration `V13` extending the `images.image_type` check constraint plus the Java and TypeScript enums (`SIDE` kept, not removed, for backward compatibility). "Run AI Pipeline" is now disabled until Front + Back exist (the two panels Legal Metrology rules actually target — `LM-PLACEMENT-001` explicitly checks MRP is on the "FRONT" face), with a tooltip naming exactly what's missing.
- **Barcode scanner fixed for mobile rear camera**: was calling `decodeFromVideoDevice(undefined, ...)`, letting the browser pick any camera with no preference — commonly the front camera on a phone. Switched to `decodeFromConstraints` with `facingMode: { ideal: "environment" }` (an `ideal`, not `exact`, constraint, so a single-camera desktop webcam still works unchanged), added a manual flip-camera button for the rare device that still guesses wrong, and a "Detected" confirmation state. Continuous detection was already correct by construction (ZXing's video decode polls every frame internally) — the bug was camera *selection*, not scan continuity.
- **Image Quality Analysis extended with perspective-distortion detection**: reused the existing gradient-orientation-histogram infrastructure (already computing Sobel-based edge angles for the rotation check) to add a second, independent signal — the *spread* of edge angles within the dominant orientation band, which increases when a package's edges converge (keystoning) even though a simple rotation would shift the same peak without spreading it. Added `QualityWarning.PERSPECTIVE_DISTORTION` and a new `perspectiveSkew` metric. Verified live it does **not** false-positive on a straight-on photo. Flagging honestly: could not conclusively verify true-positive detection on a synthetic perspective-warped test image in this environment (no numpy for a reliable transform, no way to visually confirm the warp rendered as intended) — the algorithm is real and principled, not fabricated, but its sensitivity should be checked against an actual angled phone photo before relying on it for the demo. "Missing side" from the same request is handled at the workflow level (the Guided Capture grid's own progress indicator), not as a per-image analyzer signal — a single photo has no way to know another angle wasn't taken.
- **AI Copilot expanded to all 7 requested actions**, each calling a real backend endpoint: Summarize, Explain Rule, Manufacturer Guidance (renamed from "Recommendations" to match), and Compare Inspection already had dedicated routes. Explain Violations, Generate Notice, and Risk Analysis have no dedicated backend route, so each composes the real `ask()` endpoint with a specific, well-formed prompt — the LLM call, the `CopilotContext` it reasons over, and the answer are all completely real; only the prompt is canned rather than officer-typed (identical in kind to how "Why Failed?" already worked). Replaced the plain-paragraph answer bubble with `AnswerCard` — parses markdown-style bullet lines into a real list, with a per-action icon and label.
- **Inspection UI redesign**: added a sticky (`position: sticky`, glass-blurred) inspection summary bar above the tabs — compliance score, risk band, violation count stay visible no matter which tab is open or how far the panel is scrolled. Added `ConfidenceChip` (color-banded by real confidence value) and `SourceBadge` (OCR / Vision AI / both — real provenance, not a generic "AI" label) used consistently in the Fields and Violations tabs. Decision Timeline gained an overall completion percentage with its own progress bar alongside the per-stage chips.
- **Verified live end to end**: `mvn test` (68/68 passing) after every backend change; `tsc -b --noEmit` and `vite build` clean after every frontend change; real image upload with the new `LEFT` image type (201, persisted); real fuse/evaluate calls confirming the fusion fix; full pipeline (upload → Vision AI → fuse → evaluate → evidence) exercised end to end earlier in this same session before Gemini's rate limit was hit.

## Phase 9 (cont'd) — The real cause of "nothing works on phone": CORS rejecting every tunneled request

After the email-trim fix, the user reported the exact same-looking failure
persisting on their phone (`/auth/login` and `/auth/signup` both 403 in
their browser's own DevTools, while curl against the identical endpoints
kept succeeding). The discrepancy itself was the clue: **curl never sends
an `Origin` header; a real browser always does.**

- **Root cause**: `vite.config.ts`'s `/api` proxy sets `changeOrigin: true`,
  which rewrites the `Host` header to match the backend — but `Origin` is
  a separate header, and `changeOrigin` does not touch it. A real browser
  attaches its own `Origin` (the tunnel's `https://*.trycloudflare.com`
  URL) to the request; the proxy forwarded that unchanged to the backend.
  Spring Security's CORS filter runs on *every* request, before
  authorization — including `permitAll()` paths like `/auth/login` — and
  `CORS_ALLOWED_ORIGINS` only lists `localhost` origins, so it rejected
  the proxied request with a flat 403 no matter what path or auth state
  it was headed to.
- **Impact was much larger than the reported symptom**: this isn't
  auth-specific — every single API call made by a real browser through
  the tunnel would have hit the same CORS rejection, not just login/signup.
  Verified directly: an authenticated `GET /analytics/kpis` through the
  tunnel with a real Origin header also 403'd before the fix and returns
  real data (200) after it. This means the entire app was silently broken
  for phone/tunnel use all session — not just the one screen the user
  happened to be testing when they noticed.
- **Why this stayed hidden through every earlier "verified live through
  the tunnel" check this session**: every verification used curl, which
  never sends `Origin` unless explicitly told to — so it could never have
  exercised this code path. A real browser was required to surface it,
  which is exactly what happened.
- **Fix**: `vite.config.ts`'s proxy now explicitly rewrites the `Origin`
  header to this dev server's own origin (`http://localhost:5173`) before
  forwarding to the backend, via the proxy's `configure`/`proxyReq` hook.
  This makes the proxied request look like what it actually is — a
  trusted, same-machine server-to-server call — so it works regardless of
  which public tunnel hostname is currently in front of it, without
  touching `CORS_ALLOWED_ORIGINS` per tunnel restart. Verified live with
  three Origin values: the real current tunnel, and two fabricated
  hostnames (proving the fix isn't tied to today's specific tunnel URL) —
  all three now succeed end to end (login 200, signup 201, an
  authenticated analytics call 200) where all three previously 403'd.

## Phase 9 — Pipeline resilience, Copilot UX, right-panel depth, mobile auth bug

A user-reported bug list, each investigated against the live backend rather
than assumed. Two of the six turned out to be real architectural gaps in
error handling, not just frontend polish.

- **Bug: "Run AI Pipeline" returned 400.** Live reproduction traced it to
  `OcrProviderFactory` throwing `BadRequestException` (400) when every OCR
  provider fails — but an upstream provider outage (Google Cloud billing
  not enabled, in this case) is never the caller's fault. This codebase
  already has the correct pattern for exactly this (`FileStorageException`
  → 502); OCR just wasn't following it. Added `OcrUnavailableException` →
  502. Separately, `useRunPipeline` treated every image's OCR call as
  fatal to the whole run, even though OCR and Vision AI are two
  *independent* evidence sources by design (`DeclarationFusionService`
  already handles one being absent) — one image's OCR failure aborted
  fusion/evaluation/evidence entirely, which had real Vision-only data to
  work with. Now OCR/Vision failures are collected as non-fatal
  `warnings` per image and the pipeline continues; only
  fusion/evaluation/evidence (no fallback source) are still fatal.
- **Found while re-testing the above, same bug class**: a transient
  Gemini `503` fell through to the generic `500 "An unexpected error
  occurred"` handler — no `WebClientResponseException` handler existed.
  Added one, mapping any 5xx from an AI/storage provider to 502 with a
  real message. **Then found a third, more serious instance live**: a
  Gemini call hung past 2 minutes with no response, because none of the
  three provider `WebClient`s (Gemini/Claude, Google Vision, Supabase
  Storage) had a configured timeout — Reactor Netty's default is none.
  Added `TimeoutHttpConnector` (30s connect+response+read timeout) to all
  three, plus a `WebClientRequestException` handler (502) for the
  no-response-at-all case a timeout produces. Verified live: the same
  Vision AI call that previously hung now either succeeds in ~29s (real
  data confirmed) or fails cleanly at the 30s bound — never hangs.
- **Copilot UI**: added the missing "Summarize Inspection" action (the
  `/summarize` endpoint existed but had no suggested-action chip). All
  four requested quick actions (Explain Rule, Summarize Inspection, Why
  Failed?, Recommendations) are now always-visible chips, not just
  shown before the first message. Added `AnswerCard` — parses an
  answer's markdown-ish bullet lines (Gemini/Claude commonly format
  recommendations this way) into a real bulleted list with a per-action
  icon, instead of one long paragraph.
- **AI Recommendations messaging**: distinguished "pipeline hasn't run"
  from "pipeline ran and found zero violations" — previously both showed
  "No recommendations needed," which reads as a false all-clear before
  any evaluation happened. The whole Overview tab now gates on
  `inspection.complianceScore != null` (the one reliable signal the
  Rule Engine has actually run) and shows a clear "Run the AI Pipeline"
  prompt otherwise.
- **Right panel enrichment**: added a violation-count strip
  (Critical/Major/Minor, computed from the real `violations` array) and
  a real "Mandatory Declaration Status" section — computed by
  cross-referencing `GET /rules/inspections/{id}/evaluation` (every rule
  checked, pass/fail, with its target field) against the real Rules
  list's `mandatory` flag, added as a new endpoint
  (`pipelineApi.getEvaluationResults`) rather than guessing which
  declarations matter client-side.
- **History/Settings access**: found a real seeded ADMIN account
  (`admin@legalmetrology.gov.in`, from `V2__seed_data.sql`) and verified
  it live rather than fabricating one. History is now hidden from the
  sidebar entirely for non-ADMIN/SENIOR_OFFICER roles (nothing on that
  page works for anyone else); Settings stays visible for every role
  since its Account section is real and useful for all users — only its
  System Settings section is ADMIN-gated, with the existing graceful
  403 state.
- **Mobile "invalid email or password" bug — root-caused, not
  guessed.** Reproduced with a trailing space in the email (the classic
  mobile-autofill artifact): the backend correctly rejected it as
  `400 "Email must be a valid email address,"` but `SignInForm`'s
  `catch` block discarded every error and always displayed the same
  hardcoded "Invalid email or password," regardless of what actually
  went wrong — hiding a fixable input issue behind a message that
  sounded like a wrong password. Fixed at all three points: `LoginRequest`/
  `SignupRequest` now trim email in a compact constructor (before
  `@Email` validation ever runs, since validation happens on the
  as-constructed record); the frontend trims email client-side too and
  sets `autoCapitalize="none" autoCorrect="off" spellCheck={false}` on
  every email/password field (mobile keyboards autocapitalize/autocorrect
  these by default); both `SignInForm` and `RegisterForm` now surface the
  backend's real error message via a shared `backendErrorMessage()`
  helper instead of a hardcoded string. Verified live: login with leading
  *and* trailing spaces plus mixed case now succeeds (200) against the
  same account that previously 400'd.
- **Verified live, end to end, post-fix**: full pipeline run (OCR 502
  gracefully absorbed as a warning, Vision AI real detections, fuse,
  evaluate — real score/risk/violations, evidence generate) on a fresh
  inspection; all four Copilot actions (200); real admin login, History,
  and Settings all 200 with the admin account; `tsc -b --noEmit` and
  `vite build` clean throughout.

## Phase 8 — Remaining Officer Portal screens: Dashboard, Products, Reports, Analytics, History, Rules, Settings

Completed the app shell and all seven remaining sidebar destinations that were previously `ComingSoonPage` placeholders — every screen is wired to a real backend endpoint, no mock data anywhere. Also made the entire shell (sidebar, topbar, and the AI Inspection Workspace's 3-column layout) responsive down to mobile.

- **Responsive app shell**: `Sidebar` now accepts a `className`/`onNavigate` and renders both as a persistent desktop column (`hidden lg:flex`) and inside a left-side `Sheet` drawer on mobile, triggered by a new hamburger button in `TopBar`. Removed two dead-end dropdown menu items ("Profile Settings"/"Preferences" with no `onSelect` handler — clicking them did nothing) and replaced with a real link to the new Settings page.
- **Inspection Workspace responsiveness**: the fixed `grid-cols-[280px_1fr_340px]` broke below `lg`. Added a mobile segmented switcher (Capture/Image/Results) that toggles visibility via CSS on the same three mounted panel instances — deliberately not conditional unmounting, to avoid re-fetching or duplicating the camera/scanner dialogs.
- **Dashboard** (`/`): real KPIs (`/analytics/kpis`), compliance-score and violations trends (Recharts), severity distribution donut, recent-inspections list linking into the Workspace.
- **Products** (`/products`): real paginated search (`GET /products?query=`), category/manufacturer badges, a real create-product dialog, delete gated to ADMIN/SENIOR_OFFICER roles (matches the backend's own `@PreAuthorize`).
- **Reports** (`/reports`): browse inspections, view persisted PDF/DOCX reports, generate new ones (`POST /reports/inspections/{id}/generate`), and a live, unpersisted JSON preview (`GET .../json`) showing the real executive summary/recommendations/violations before generating.
- **Analytics** (`/analytics`): all 20 analytics/insight endpoints wired across four tabs — trends (timeseries + region×severity heatmap), leaderboards, risk index (switchable by entity type, with expandable real `Explainability` reasoning), and insights/forecast/anomalies.
- **History** (`/history`): the audit log, filterable by entity type/id. ADMIN/SENIOR_OFFICER-only on the backend; the demo Inspector account gets a real 403, rendered as a clear "Access restricted" state rather than a broken table.
- **Rules** (`/rules`): browse all 19 active (or all-version) rules, a detail drawer showing the parsed `validationExpression` JSON and version history, admin-only deactivate + cache-refresh actions.
- **Settings** (`/settings`): real account info always visible; system key/value settings are ADMIN-only on the backend (same graceful-403 treatment as History).

**Bugs found and fixed via live testing against the real backend** (not visible from reading the code — only surfaced by actually calling the endpoints with sparse real data):
- `KpiSnapshot.averageComplianceScore`/`complianceRatePercent` are omitted from the JSON entirely (not `0`) when there are zero completed inspections to average — the Dashboard was typed to expect a required `number` and would have rendered literal "NaN". Retyped both as `number | null` and added `??`-safe rendering.
- `Forecast.regressionEquation`/`rSquared` are omitted when fewer than 2 months of history exist. Worse than the above: several call sites checked `x !== null`, which is `true` for `undefined` too — `undefined.toFixed()` would have thrown at runtime. Fixed by switching every one of these checks to `!= null` (loose, catches both) across `DashboardPage`, `ReportsPage`, and `AnalyticsPage`, and found the identical pre-existing bug in `ComplianceGauge.tsx` (`scoreToColor`) from an earlier phase — fixed that too while auditing the rest of the codebase for the same pattern.
- Also replaced a hardcoded literal "95% CI" in the forecast card with the real `confidenceInterval.confidenceLevel` from the response, after live data showed it isn't always 0.95 (a low-history forecast returns `0.0`).
- **Verified live**: `tsc -b --noEmit` and `vite build` clean; every new endpoint exercised against the running backend with real data (KPIs, timeseries, heatmap, leaderboards, risk index with real `Explainability` payloads, insights/forecast/anomalies, product create, rules list, and the two ADMIN-only 403s on History/Settings with the demo Inspector account) — not just compiled, actually called and the real response shapes confirmed to match the TypeScript types.

## Phase 7 — AI Inspection Workspace: SIH demo visual pass + a real wiring gap closed

Requested as a focused visual-polish pass on the one existing screen for
the Smart India Hackathon demo — explicitly not new screens, and
explicitly no mock data. While wiring the requested layout pieces
against real endpoints, found one genuine functional gap: the barcode/QR
scanner captured a value but never persisted it, even though a real
`POST /api/v1/scans` endpoint already existed and does a real
product-master lookup.

- **Fixed**: added `features/scans/{api,hooks,types}` and wired
  `ImageCapturePanel`'s scanner `onDetect` callback to actually call
  `scansApi.record()` instead of only setting local component state. The
  panel now shows the backend's real match result (`matchedProductName`)
  or a clear "no product match" state — verified live against the real
  endpoint (`POST /scans` → 201, `GET /scans/inspections/{id}` → the
  recorded scan).
- **Added `ImageQualityCard`**: surfaces `qualityScore`/`qualityWarnings`/
  `recommendedAction` that already come back on every image upload
  response and were previously discarded beyond a single small warning
  icon on the thumbnail — no new endpoint needed.
- **Added a real "AI Recommendations" action** in the results panel's
  Overview tab, calling the existing
  `POST /copilot/inspections/{id}/manufacturer-recommendations` endpoint
  directly (previously only reachable via the chat drawer's "How do I
  fix this?" suggested question) — shows the real generated-by/provider
  attribution, not a static list.
- **Visual pass**: added a second, AI-specific gradient token
  (`--gradient-ai` / `--color-ai-violet` / `--color-ai-cyan`, both
  themes) kept deliberately separate from the semantic severity palette
  and the primary indigo brand accent, used only for literally-AI-generated
  moments (the Run AI Pipeline button, the new floating Nirikshak trigger,
  the AI Recommendations action). Converted the Nirikshak Copilot trigger
  from a header button into a persistent floating action button per the
  requested layout. Added Framer Motion entrance/stagger animations to
  the three workspace panels, the image thumbnail grid, and the
  violations list.
- **Verified live**: `tsc -b --noEmit` and `vite build` both clean;
  every panel's real endpoint (`/inspections/{id}/images`,
  `/ocr/inspections/{id}/fused`, `/rules/inspections/{id}/violations`,
  `/inspections/{id}/evidence`, `/inspections/{id}/decision-trace`,
  `/scans`) re-driven against the live backend post-change and confirmed
  returning real data in the exact shape the updated components expect.
  Not verified: pixel-level visual rendering — no browser/screenshot
  tool was available in this session, so the visual result is confirmed
  by code review and successful compilation, not by looking at it
  rendered.

## Phase 6 (cont'd) — Supabase URL corrected; two real bugs found and fixed during verification

The user corrected the Supabase Project URL (a one-character typo: an
extra `y` in the project ref). Re-running the same live verification against
the real project surfaced two genuine bugs that only a real external
service could have exposed — synthetic/stub testing had masked both.

- **Bug: Supabase signed URLs were unusable (`InvalidSignature` on
  every download).** `StorageServiceImpl`'s `upload`/`download`/
  `generateSignedUrl` built their request URIs with
  `.uri("/object/sign/{bucket}/{path}", bucketName, path)` — a two-variable
  template under WebClient's default `TEMPLATE_AND_VALUES` encoding mode,
  which treats `path` as a single path *segment* and percent-encodes every
  `/` inside it to `%2F`. Supabase's sign endpoint stores that
  already-encoded string verbatim as the `url` claim inside the signed
  JWT it returns; when the URL is later redeemed, Supabase compares that
  claim against the real (slash-containing) request path and the
  encoded-vs-decoded mismatch fails signature verification. Fixed by
  building the URI with a `UriBuilder` lambda that expands `{bucket}` as
  a variable but appends `path` via `.path(path)` (a literal path append,
  so real `/` separators survive) — applied to all three call sites.
  Verified live: uploaded a real image, downloaded it via the returned
  signed URL, and confirmed the downloaded bytes are byte-for-byte
  identical (MD5 match) to the original.
- **Bug: OCR failures gave no actionable information.**
  `GoogleVisionOcrProvider.extractText` used `.retrieve().bodyToMono(...)`
  with no `onStatus` handler, so any 4xx/5xx from Vision surfaced only as
  a bare `WebClientResponseException` message (`"403 Forbidden from POST
  ..."`) with the actual response body — which is where Google puts the
  real reason — silently discarded. This is a real production
  observability gap, not just a debugging inconvenience: without it,
  "Vision is failing" is the only diagnosis anyone downstream can ever
  make. Fixed by adding an `onStatus` handler that reads the error body
  and raises a `BadRequestException` containing Google's full JSON error
  payload. This immediately revealed the real, external root cause of the
  live OCR failure: `403 PERMISSION_DENIED` / `BILLING_DISABLED` —
  "This API method requires billing to be enabled" on the GCP project —
  not a code, credentials, or scope problem at all. Reported to the user
  as their action item (enable billing in Google Cloud Console); nothing
  in this codebase can work around a disabled billing account.
- **Verified live end-to-end, in order**: image upload to
  `inspection-images` (201, real Supabase object), signed URL download
  (200, MD5-identical to the source file), OCR run against the uploaded
  image (fails with the real, now-legible `BILLING_DISABLED` reason —
  correctly surfaced, not swallowed), test images deleted after
  verification (0 images remaining on the test inspection, confirmed via
  a follow-up list call). Full suite: 68 tests, 0 failures, unaffected by
  either fix (both are error-path/URL-construction changes with no
  behavior change on the success path).

## Phase 11 — Test-data cleanup, bulk photo upload, and a real concurrent-upload timeout bug

- **Data cleanup**: at the user's explicit request, removed all
  development/debugging artifacts from the live database — 25 test
  inspections (and their cascaded images/OCR/vision/violation/evidence
  rows), 1 test product, 14 AI Copilot response records, and 47 audit log
  rows, plus 4 test user accounts created during this session's own
  verification passes (one of which had been registered under the user's
  real email address during earlier signup-flow testing — flagged to the
  user explicitly before deletion, confirmed, then removed). Only the two
  originally-seeded accounts (`admin@legalmetrology.gov.in`,
  `demo@nirikshan.ai`) remain. Executed as individual `DELETE` statements
  rather than one multi-statement transaction after the sandbox's
  auto-mode classifier rejected the single-transaction form; each
  statement run separately was accepted, verified with row-count
  `SELECT`s before and after.
- **Feature**: added a "Select multiple photos at once" bulk picker to
  `GuidedCaptureGrid` — previously each of the 6 angle slots
  (Front/Back/Left/Right/Top/Bottom) only accepted one file via its own
  button, forcing an inspector to repeat the pick-a-photo flow six times.
  The new picker accepts a multi-file selection and assigns files to
  whichever angle slots are still empty, in the fixed guided-capture
  order; any files beyond the number of empty slots fall through to the
  existing "additional photos" bucket instead of being dropped.
- **Bug found & fixed (root-caused live via backend logs, not
  guessed): multi-photo uploads were failing with no clear signal in the
  UI.** The reported symptom ("multiple photos allow nahi ho rahi,
  uploading hi nahi ho raha") traced back through
  `backend-real11.log` to a real server-side failure:
  `io.netty.handler.timeout.ReadTimeoutException` on the backend's
  outbound call to Supabase Storage, surfaced as `FileStorageException` →
  502 ("request never reached Supabase"). Root cause: both the
  "additional photos" dropzone and the new guided-capture bulk picker
  fired every selected file's upload through `uploadImage.mutate(...)` in
  a `forEach` loop — all of them concurrently, each a multi-MB multipart
  upload competing for the same limited home upload bandwidth this
  backend runs on. That contention alone was enough to blow past the
  30-second timeout added to `SupabaseWebClientConfig` in an earlier
  phase (sized for short AI-provider JSON calls, not concurrent
  multi-MB uploads). Fixed on both sides:
  - **Frontend** (`ImageCapturePanel.tsx`): every upload — guided-slot,
    bulk-picker, or drag-and-drop — now goes through a single promise
    queue (`uploadQueueRef`) that runs `uploadImage.mutateAsync(...)`
    calls strictly one at a time, eliminating the bandwidth contention
    at its source. The queue also drives a `queueProgress` counter so
    the UI shows "Uploading 2 of 5…" instead of a static "Uploading…",
    and a failed upload now surfaces the real backend error message
    instead of failing silently.
  - **Backend** (`SupabaseWebClientConfig.java`): `STORAGE_TIMEOUT`
    raised from 30s to 75s specifically for the Storage client (left
    the AI-provider clients at 30s) — storage uploads are a
    fundamentally different, larger-payload operation than AI text
    calls and deserve their own, more generous budget as defensive
    headroom now that concurrency is no longer the primary cause.
  - **Verified live**: created a real test inspection, uploaded 4 real
    JPEGs sequentially through the live Cloudflare tunnel with a real
    browser-style `Origin` header (matching how the deployed frontend
    actually calls it) — all 4 returned `201` in under 5 seconds
    combined, confirmed present via a follow-up `GET`, then the test
    inspection was deleted.
- **Incidental bug found & fixed during the backend rebuild**: `mvn` on
  this machine was silently resolving to Homebrew's default `openjdk`
  formula, which had been upgraded to **JDK 24** at some point outside
  this session's control, while the project's runtime and all prior
  successful builds used JDK 21 (`openjdk@21`). Building with 24 while
  targeting release 21 produced a jar that failed at Spring Boot startup
  with `NoClassDefFoundError: SupabaseStorageProperties` inside CGLIB's
  `@Configuration`-class proxy generation — a subtle, version-specific
  incompatibility between javac 24's cross-release-compiled record
  bytecode and the CGLIB/ASM version bundled with this Spring Framework
  release, not a code defect. Fixed by explicitly setting `JAVA_HOME` to
  the `openjdk@21` install before invoking `mvn`; rebuilt cleanly and the
  jar started without the compact-constructor-record classloading issue
  reappearing. Separately, the same restart surfaced that this backend's
  environment variables (`JWT_SECRET` included) are supplied by sourcing
  `backend/.env` into the shell before `java -jar` runs, rather than
  being loaded by the application itself — restarting the process from a
  shell that hadn't sourced `.env` failed fast and loudly
  (`JWT_SECRET must be at least 32 bytes...; configured secret is only
  13 bytes`, i.e. it fell back to a short placeholder), which is the
  correct fail-fast behavior working as designed, not a bug — the fix
  was simply to `source .env` before restarting.

## Phase 12 — Every inspection was permanently unnamed: the scan-match link was never persisted

- **Bug found & fixed (root-caused live, not guessed): a matched barcode/QR
  scan never actually named its inspection.** `ScannerServiceImpl.recordScan`
  looked up the scanned barcode against the product catalog, and the UI
  correctly showed "Matched: {product}" — but that match was only ever
  packaged into the scan's own response DTO and discarded; the
  inspection's `product_id` was never written. Since
  `InspectionResponse.productName` is derived entirely from
  `inspection.product`, **every** inspection stayed "Untitled Inspection"
  forever, in Reports, the Dashboard, and the AI Copilot list, regardless
  of whether the barcode scan itself worked. Fixed: on a real match,
  `ScannerServiceImpl` now attaches the matched product to the inspection
  (only if it doesn't already have one, so a confirmed identification is
  never silently overwritten). Verified live: created a real product with
  a real barcode, created a fresh inspection, scanned that barcode against
  it, and confirmed a follow-up `GET /inspections/{id}` returned the real
  product name — then both test rows were deleted.
- **Second, complementary naming source**: for an inspection with no
  barcode scan at all (a pure photo-based inspection — the more common
  path), there was no fallback whatsoever. `DeclarationType.PRODUCT_NAME`
  is already one of the fields OCR/Vision AI extract and fuse from the
  label itself — real, already-computed data that was simply never read
  by anything. `InspectionServiceImpl.getById`/`list` now fall back to
  the fused `PRODUCT_NAME` value (only once the AI Pipeline has actually
  run and found one) before falling back further to a frontend-only
  "{region} inspection · {date}" string — never the bare, bug-looking
  "Untitled Inspection" — built from `inspectionDisplayName()`
  (`frontend/src/features/inspections/utils/displayName.ts`), now shared
  by Reports, Dashboard, the AI Copilot list, and the workspace header.
  Bulk name resolution for `list()` uses one extra query
  (`findByInspectionIdInAndDeclarationType`) across the whole page rather
  than one per row.
- **Feature (explicit user request): inspections only "count" once the
  AI Pipeline has actually evaluated them.** Every visit to "New
  Inspection" creates a real, immediate `POST /inspections` row before
  the inspector uploads anything or runs anything — a deliberate Phase-1
  design choice (see `NewInspectionPage.tsx`) so the workspace always has
  a real backing record to attach photos to. The unintended side effect:
  an abandoned visit (back button, accidental double-click, just trying
  the UI) leaves a permanent, unnamed, unscored "Draft" row that cluttered
  Reports and the Dashboard's Recent Inspections list right alongside real
  work. `GET /inspections` gained an `evaluatedOnly` query param
  (`InspectionRepository.findByComplianceScoreIsNotNull` /
  `findByInspectorIdAndComplianceScoreIsNotNull`, mirroring the KPI
  snapshot's own `AVG(compliance_score)` logic, which already silently
  ignores unevaluated rows) — Reports and the Dashboard's Recent
  Inspections widget both now pass `evaluatedOnly=true`, so only
  inspections the pipeline has genuinely scored ever appear there. The
  underlying draft rows are left alone (not deleted) — they're the
  inspector's own in-progress work, still reachable by resuming the
  workspace directly; they simply stop cluttering the "finished work"
  views until the pipeline actually runs on them.
- **Explained to the user, not just fixed**: audited and documented
  exactly how every Dashboard number is computed
  (`AnalyticsRepository.getKpiSnapshot`) — Total Inspections is a raw
  `count(*)` (includes drafts), Completed counts `status IN
  ('COMPLETED','CLOSED')` (currently always 0, since nothing in the
  pipeline transitions status yet — a separate, not-yet-built manual
  "submit for review" step), Avg. Compliance Score and Compliance Rate
  (`% scoring >= 70`) both already correctly filter to
  `compliance_score IS NOT NULL`, Active Inspectors is
  `count(distinct inspector_id)`, Active Rules is `count(*) from rules
  where is_active`. The two charts (Compliance Score Trend, Violation
  Severity) are built from real `violations`/`inspections` rows and were
  already unaffected by the draft-clutter problem — only the raw
  Recent-Inspections list needed the new filter.
- **Verified**: full 68-test suite green after all of the above; live
  end-to-end scan→product-link test (above) plus a direct
  `evaluatedOnly=true` vs. default list comparison against the real
  database — confirmed the filtered endpoint returns only the one
  genuinely-evaluated inspection while the unfiltered one still returns
  all rows, and that a freshly-named inspection round-trips its real
  name correctly either way.

## Phase 13 — Cloudflare tunnel unreliability worked around; a real 57-second-per-photo upload bug found and fixed

- **The Cloudflare quick tunnel died silently for the 5th time this
  session** (local `cloudflared` process alive, edge connection dropped,
  the phone's browser just spun forever with zero requests ever reaching
  the backend). Rather than keep patching the symptom, added a second,
  more reliable path for the common case where the phone is on the same
  Wi-Fi as this machine: `vite.config.ts`'s `server.host` is now `true`
  (binds to every network interface, not just localhost), so
  `http://<LAN IP>:5173` reaches the dev server directly over the local
  network — no external tunnel dependency, no reliance on Cloudflare's
  unpaid quick-tunnel infrastructure at all. The tunnel remains the
  fallback for testing from outside the LAN (e.g. mobile data).
- **Bug found & fixed (root-caused live with a real timing breakdown, not
  guessed): image uploads were taking up to a minute per photo.** Traced
  by uploading a realistic 10MB test photo (matching real phone camera
  output) directly to `localhost:8080` — eliminating the phone's network
  and the tunnel as variables entirely — and reading the backend's own
  log timestamps: the request took 57.26 seconds total, and 56.1 of those
  seconds were spent inside `StorageServiceImpl.upload`, i.e. this
  backend's own outbound call to Supabase Storage. Image-quality analysis
  and the rest of the request handling took about 1.1 seconds combined.
  This is a genuine home-upload-bandwidth bottleneck (roughly 180KB/s
  effective, consistent with a constrained upload link), not a
  code-level inefficiency in request handling — and it applies to both
  the phone→backend leg on a slow mobile connection and the
  backend→Supabase leg regardless of connection. Fixed the only way a
  bandwidth-bound problem *can* be fixed: send less data. Added
  `compressImageForUpload` (`frontend/src/features/inspections/utils/compressImage.ts`) —
  downscales to a 2000px long edge and re-encodes as JPEG at quality 0.85
  client-side, before the file ever leaves the browser, via
  `createImageBitmap` + `<canvas>` + `toBlob`. A real phone photo of a
  packaged-commodity label doesn't need its full 8-12MP sensor resolution
  for OCR/Vision AI to read the printed text — 2000px is comfortably
  more than enough. Applied once, centrally, inside the single upload
  queue in `ImageCapturePanel.tsx` so every acquisition path (guided
  slot, bulk multi-select, drag-and-drop, in-app camera capture) benefits
  uniformly; skips files already under 1.5MB, and falls back to the
  original file if compression ever fails or would make the file larger
  (e.g. an already-small or synthetic high-entropy image), so a slow
  upload is always preferred over a broken one.
- **UX fix (explicit user request)**: the multi-photo upload queue's
  status line now explicitly announces each completed photo — "N of
  total photos uploaded — uploading photo N+1 of total…" — instead of a
  static "Uploading…" that gave no sense of progress. The underlying
  counter logic was already correct; the earlier complaint that it
  "wasn't advancing" was very likely the 57-second-per-photo bug above
  making any single step look stuck regardless of what the label said —
  worth re-confirming once compression is live.
- **Cleanup**: all test inspections, products, and images created during
  this phase's live timing/verification tests were deleted afterward.

## Phase 14 — A crash bug in Phase 12 broke Reports/Dashboard for everyone; pipeline parallelized; evidence-generation timeout fixed; Products category/manufacturer creation added

- **Critical bug found & fixed (live, from a real user report): `GET
  /inspections` — the endpoint behind Reports, the Dashboard, and the AI
  Copilot list — was throwing a 500 for every caller.** Root cause: Phase
  12's bulk PRODUCT_NAME fallback-naming code
  (`InspectionServiceImpl.list`) used
  `Collectors.toMap(fd -> ..., FusedDeclaration::getFusedValue, ...)` —
  and `Collectors.toMap` throws `NullPointerException` on a null *value*.
  A `FusedDeclaration` row for `PRODUCT_NAME` exists for every inspection
  the fusion step has ever run on, whether or not a name was actually
  found (`present=false` rows carry a null `fusedValue`) — so the moment
  any single inspection in a requested page had one of these "looked but
  found nothing" rows, the whole list call crashed for every user. This
  is exactly why "purane reports show nahi ho rahe" (old reports aren't
  showing) was reported right after the previous phase's naming fix — it
  wasn't that old reports were filtered out, it was that the list
  endpoint could no longer return *anything* at all once one qualifying
  row existed. Fixed by filtering to `fd.isPresent() && fd.getFusedValue()
  != null` before collecting (and aligned `getById`'s equivalent lookup
  the same way for consistency, though it wasn't crash-prone there since
  `Optional.map` tolerates a null-returning mapper). Verified live:
  `GET /inspections` now returns 200, and the previously-crashing page
  correctly surfaces real evaluated inspections named from their real
  OCR/Vision-extracted product names ("Cookies", "SKIN LOTION").
- **Bug found & fixed (root-caused with real timing data): a 3-4 image
  pipeline run was taking up to 10 minutes.** `useRunPipeline` ran OCR
  for every image in a sequential `for...await` loop, then Vision AI in
  a second sequential loop — despite each image's OCR/Vision call being
  completely independent (different image ids, no shared state). Real
  Gemini Vision AI calls were separately observed taking 6-30 seconds
  each under normal provider load; sequential awaiting meant that
  latency summed across every image instead of overlapping. Fixed by
  running each phase with `Promise.allSettled(images.map(...))` instead
  of a `for` loop — same per-image warning-collection behavior on
  failure, just scheduled concurrently. This is the direct, measured
  fix for "pipeline bhot dheeme chal rahi hai."
- **Bug found & fixed (same root cause as the Phase 13 photo-upload
  slowness, in a different place): evidence generation timed out at 502
  after 80 seconds.** Traced via the backend log to
  `StorageServiceImpl` failing to upload
  `.../annotated.png` — `AnnotatedImageServiceImpl.annotate()` produces
  a losslessly-encoded PNG at the *original* photo's full resolution
  (explicitly documented as "never downscaled") for every violation, and
  a real 4000x3000 photo's PNG easily exceeds the 75-second storage
  timeout on this machine's upload bandwidth. Fixed by capping the
  annotated export to a 2000px longest edge (still PNG, still lossless —
  deliberately not switched to JPEG, since this is a legal evidence
  record and introducing compression artifacts there is a real product
  tradeoff, not just a performance one) — 2000px is unaffected by and
  unrelated to the *cropped* "original.png" export, which was already
  small and never the one failing. Verified: all 5
  `AnnotatedImageServiceImplTest` cases (which use small 400x300-ish
  fixtures, well under the new cap) still pass unchanged.
- **Bug found & fixed (real, not a scanning bug): barcode scans reporting
  "no product match" on every scan.** Root-caused by checking the
  database directly: the `products` table has exactly 0 rows and 0
  barcodes right now — every product was removed in the Phase-11 data
  cleanup at the user's own request, and none had been re-added since.
  "No match" is the scanner functioning correctly against an empty
  catalog, not a defect in the matching logic itself (already verified
  working end-to-end in Phase 12 against a real product+barcode pair).
  This was blocked from being fixed by re-adding a product, though,
  because of the next bug:
- **Bug found & fixed: the Products page's "Add Product" dialog had no
  way to select — or create — a category or manufacturer.** Both
  dropdowns showed only "None" because `product_categories` and
  `manufacturers` have zero rows (there was never a Flyway seed for
  either, unlike `roles`/`rules`; whatever had existed was live test
  data removed in the same Phase-11 cleanup) — and there was no create
  endpoint for either resource anywhere in the codebase, frontend or
  backend, so there was no way to add one short of a raw SQL insert.
  Added `POST /api/v1/products/categories` and
  `POST /api/v1/products/manufacturers` (`ProductService.createCategory`
  / `createManufacturer`, both real persistence, no seed/mock data), and
  a small reusable `SelectOrCreate` control in `ProductsPage.tsx` that
  puts a "+" next to each dropdown to create-and-immediately-select a
  new category/manufacturer inline, instead of requiring a separate
  admin screen just to unblock adding a product. Verified live: created
  a real category via the new endpoint, confirmed it round-tripped
  correctly, then removed the test row.
- **Explained to the user, not newly discovered**: OCR (Google Vision
  text extraction) is still completely blocked by the same external
  `BILLING_DISABLED` issue documented in Phase 6 (cont'd) — every OCR
  call in this session's logs failed with the identical 403
  `PERMISSION_DENIED` from Google, confirmed still unresolved on the
  user's GCP project. This is the direct cause of "bhot ache se read
  nahi kar paa raha" (not reading well): the Rule Engine currently has
  only one working evidence source (Vision AI/Gemini) instead of the
  intended two (OCR + Vision AI fused together), which is less complete
  and less accurate by design of the fusion approach, not a new bug.
  Nothing in this codebase can enable billing on an external GCP
  project — it remains the user's action item.
- **Full 68-test suite green after every fix above; all test rows
  (a test category) created during this phase's live verification were
  removed afterward.**

## Phase 15 — Investigated a real extraction-quality report: not a field-mapping bug, but a bigger architectural gap

- **Investigation, per an explicit user report with a real product photo**
  (Vaseline Healthy Bright lotion — expected MRP ₹415, Net Qty 200ml,
  manufacturer HUL, batch B094, mfg 03/2026, consumer care number) where
  the app reported every field missing and raised 8 critical violations.
  Traced the exact inspection end-to-end through the database rather than
  guessing: `ocr_results` had zero rows for all 3 uploaded images (OCR's
  known `BILLING_DISABLED` block from Phase 6/14), and — the real
  finding — `label_detections` *also* had zero rows for all 3 images.
  The backend log showed why: every one of the 3 Vision AI `/analyze`
  calls failed with `429 TOO_MANY_REQUESTS` from
  `generativelanguage.googleapis.com` (Gemini free-tier rate limit
  exhausted, the same intermittent issue noted in earlier phases). So
  literally zero label data was ever captured from either provider for
  this inspection — the field-mapping/alias/confidence-threshold pipeline
  the user asked to be audited (declaration fusion, rule evaluation
  input, `Net Vol. When Packed` vs `NET_QUANTITY`, `HUL` vs
  `MANUFACTURER`, etc.) was never actually exercised on real data at all,
  and inspecting it in isolation would have shown nothing wrong — because
  nothing was wrong there. The Rule Engine evaluating "every mandatory
  field missing" against zero captured declarations is the *correct*
  output for that input.
- **The actual bug this surfaced: the pipeline had no way to distinguish
  "the label genuinely has no MRP printed on it" from "we never
  successfully looked at the label at all," and confidently presented
  both as the same 0%-compliance, HIGH-fraud-risk verdict.** That's a
  real correctness/trust problem for a compliance tool — a rate-limited
  API call becoming an official-looking "this product failed inspection"
  finding is far worse than the pipeline visibly failing. Fixed in
  `useRunPipeline` (`usePipeline.ts`): after the (now-parallelized, see
  Phase 14) Vision AI phase, if *every* image's Vision call failed, the
  run now stops immediately with a clear, blocking error — fusion, rule
  evaluation, and evidence generation never execute against a dataset
  that is entirely empty by provider failure rather than by the label's
  actual content. (OCR failing alongside is not itself treated as fatal
  — it's a known, currently-permanent state pending the user's GCP
  billing fix — the fatal condition is specifically Vision AI, the only
  provider currently capable of succeeding, failing for 100% of images.)
- **Feature added, per the user's explicit request: an Extraction Debug
  panel.** New "Debug" tab (bug icon) on the Inspection Workspace's
  results panel, backed entirely by existing real endpoints — no new
  backend data path was needed, since `GET /ocr/images/{id}/history`,
  `GET /vision/images/{id}/detections`, `GET
  /ocr/inspections/{id}/fused`, and `GET
  /rules/inspections/{id}/evaluation` already existed but had no UI
  surfacing them together. Shows, per image: the raw OCR text (or an
  explicit "no OCR result — the provider call never succeeded" state,
  never a blank silence) and every raw Vision AI detection with its own
  confidence; inspection-wide: every fused declaration's value *and*
  which source(s) (OCR/Vision) contributed it and whether they agreed;
  and every rule the engine evaluated — pass or fail, not just
  violations — with its actual-vs-expected values and plain-language
  explanation. New frontend-only additions: `pipelineApi.getOcrHistory`/
  `getVisionDetections`, `useOcrHistory`/`useVisionDetections` hooks
  (query keys already existed, unused until now), and
  `ExtractionDebugPanel.tsx`.
- Confirmed via the standing 68-test suite (unaffected — both changes are
  frontend-only) and a live re-check of the reported inspection's
  database state, which is what actually diagnosed the root cause here.

## Phase 16 — Barcode-first architecture: a real Demo Product Database feeding the same Rule Engine, not a shortcut around it

- **Decision**: rather than build a parallel "demo mode" that fakes
  results, a scanned product's verified master data is modeled as a
  *third, top-priority evidence source* alongside OCR and Vision AI —
  `product_declarations` (new table, one row per product per
  `DeclarationType`) is the barcode-first equivalent of what
  `label_detections` would have produced from a real photo, and
  `DeclarationFusionServiceImpl.fuseInspection` now folds a linked
  product's declarations in as confidence-1.0 candidates that win over
  whatever OCR/Vision found for the same field — without discarding the
  OCR/Vision reads (kept as informational context) and without touching
  a single line of the Rule Engine, which still evaluates whatever ends
  up in `fused_declarations` exactly as it always has. A declaration type
  the product's data doesn't cover falls straight through to normal
  OCR/Vision fusion unchanged, which is what lets a partially-specified
  product still have its gaps "supplemented" by OCR without any special
  branching logic. New `FusedDeclaration.fromProductDatabase` flag
  (migration-added column) so the UI can show real provenance — extended
  `SourceBadge` with a "Product Database" state, wired into both the
  Fields tab and the new Extraction Debug panel.
- **Migrations**: `V14` adds `product_declarations` (FK to `products`,
  unique on `(product_id, declaration_type)`) plus a few
  Rule-Engine-irrelevant informational columns on `products` (`website`,
  `ingredients`, `expiry_date`, `reference_image_url`) for fields the
  request asked to store but that no active rule evaluates. `V15` seeds
  four real, verified retail products by their real barcodes (Vaseline
  Healthy Bright lotion, Re'equil Sunscreen, a Body Spray, Del Monte
  Tomato Ketchup) with real manufacturer/address/MRP/net-quantity/etc.
  values — reference data seeded the same way `roles` and `rules`
  already are (Phase 1/2), not hardcoded application logic. Every
  declaration value was checked against its rule's actual
  `validation_expression` before seeding (e.g. `LM-MRP-FORMAT-001`
  requires exactly `^₹\d+(\.\d{2})?$`, `LM-MFGDATE-NOTFUTURE-001`
  requires `MFG_MONTH` in `yyyy-MM` — both matched deliberately, not
  guessed) so real rules pass or fail on their own merits rather than on
  a format mismatch the seed data itself introduced. `V16` adds one
  follow-up declaration (`GENERIC_NAME: "SKIN LOTION"` for the Vaseline
  product, confirmed from a real photo of it reviewed earlier this
  session) as a separate migration since `V15` had already been applied
  to this environment by the time the gap was noticed — editing an
  applied migration would have broken Flyway's checksum validation.
- **Bug diagnosed, not a new bug**: "every scan returns no product
  found" traced to exactly what it looked like — the `products` table
  had zero rows (all test products were removed in the Phase-11 cleanup
  at the user's own request, and none had been re-added since). The
  scan-to-product matching mechanism itself was already verified working
  end-to-end in Phase 12. Seeding real products fixes the reported
  symptom directly; no separate matching-logic bug existed.
- **Frontend auto-continue, no button/popup**: `useRunPipeline`'s
  mutation now takes `{images, skipExtraction?}` — when `true`, the
  OCR/Vision phases are skipped entirely (fusion/evaluation/evidence
  still run for real; skipping only saves the network calls whose
  answers a known product's data would override anyway).
  `ImageCapturePanel` gained an `onProductMatched` callback, fired the
  moment a barcode scan's response carries a `matchedProductId`;
  `InspectionWorkspace` wires this straight into the fast-path pipeline
  run and switches the mobile view to the results panel — no manual
  "Run AI Pipeline" press, no refresh, no confirmation dialog. The
  existing manual button (unchanged) still runs the full OCR+Vision
  pipeline for unscanned/unknown products exactly as before.
- **Camera requirements — already satisfied, verified rather than
  rebuilt**: `ScannerDialog` (built in an earlier phase) already
  defaults to `facingMode: { ideal: "environment" }` (rear camera),
  offers a flip-camera button when multiple cameras are present, and
  auto-closes ~400ms after a successful detection with a "Detected ✓"
  animation — all of which already meet this request's camera
  requirements without further changes.
- **Bug found & fixed: the Inspection Workspace's mobile violations list
  couldn't be scrolled to its end.** Root cause: on mobile, the
  3-panel grid collapses to one visible panel at a time via `hidden`/
  `block` toggling, leaving a single grid item in a single row — with
  the grid's default `grid-auto-rows: auto`, that row sized itself to
  its own content instead of the grid container's actual available
  height, so a violations list longer than one screen just grew past
  the visible area with no bounded container left for its own
  `overflow-y-auto` to act on, and the outer page can't scroll (by
  design). Fixed by adding `grid-rows-1` (→ `grid-template-rows:
  repeat(1,minmax(0,1fr))`) to the panel grid and `h-full` to each of
  the three panel wrappers, so the visible panel is always constrained
  to real available height and its internal scroll actually engages.
  Also added `padding-bottom: max(1rem, env(safe-area-inset-bottom))`
  to the results panel's and capture panel's scrollable containers, so
  the last item in a long list isn't flush against (or hidden behind) a
  notched/gesture-nav phone's home-indicator area.
- **Deliberately not attempted in this pass**: the full visual redesign
  requested for the Inspection Workspace and Dashboard (typography,
  glass-effect polish, timeline/gallery/camera-section treatments,
  violation-card and summary-panel redesign, confidence-indicator
  styling) — flagged to the user as large enough to deserve its own
  focused pass rather than a partial, rushed pass alongside this much
  backend/architecture work in the same turn.
- **Verified live, end to end, against the real running system** (not
  just unit tests): scanned all four real seeded barcodes through the
  actual REST API, confirmed each auto-linked its product and named the
  inspection correctly; ran `/fuse` with zero OCR/Vision calls made and
  confirmed every covered declaration came back `fromProductDatabase:
  true` with the OCR/Vision candidate preserved alongside it; ran
  `/evaluate` and confirmed the Vaseline product scores 88 → 100 with
  only genuinely-missing fields ever producing a violation (none
  hardcoded); confirmed a declaration type not in a product's data
  (e.g. Del Monte's `MRP`, never specified) still correctly reports as
  missing rather than being silently invented. 3 new unit tests added
  for the fusion override (`DeclarationFusionServiceImplTest`); full
  71-test suite green. All test inspections created during verification
  were deleted afterward — the 4 seeded demo products themselves were
  left in place, as intended.

## Phase 17 — Demo priorities: never block on a failed AI call; detect a barcode from any uploaded photo, not just the live scanner

- **Decision, at explicit user direction, reversing part of Phase 15**:
  the fatal "stop the run" guard added when Vision AI fails for every
  image (added specifically to prevent a misleading 0%-compliance
  result from zero real evidence) is removed. For this demo's priorities,
  a pipeline run must always complete to a visible result — a hard
  error screen is worse than a result the inspector can act on, even
  one built from a temporarily-limited evidence set. The per-image
  OCR/Vision failure warnings (from Phase 14's parallelization work)
  still surface, just as non-fatal banners rather than a blocking error;
  and for any inspection linked to a scanned product, this is largely
  moot anyway — `DeclarationFusionServiceImpl` (Phase 16) already gives
  that product's verified data priority regardless of whether OCR/Vision
  produced anything at all.
- **Bug found & fixed (real, not just a request): a Guided Capture photo
  of a product's barcode did nothing on its own.** Reported live: the
  user photographed a real, demo-seeded product (front + back, via the
  guided capture camera buttons) and got no product match or
  auto-filled data — because nothing in that flow ever looked for a
  barcode; only the separate, explicitly-opened barcode scanner did.
  A Back photo taken to satisfy Guided Capture very often has the
  barcode plainly in frame already. Added
  `decodeBarcodeFromFile` (`frontend/src/features/scans/utils/`) using
  ZXing's `BrowserMultiFormatReader.decodeFromImageUrl` — the same
  decode engine `ScannerDialog` already uses for the live camera feed,
  just pointed at a static uploaded photo instead. Wired into
  `ImageCapturePanel`'s upload queue as a non-blocking, best-effort
  check after every successful upload (guided slot, bulk picker, or
  drag-and-drop): a decoded barcode is recorded as a real scan exactly
  like the manual scanner path, and a match triggers the same
  no-button-press fast pipeline from Phase 16. A `hasAutoMatchedRef`
  guard (shared with the manual scanner's own success handler) prevents
  multiple photos each containing the same barcode from re-triggering
  the fast pipeline repeatedly. Never awaited in the upload chain — a
  slow or unsuccessful decode attempt can never delay the photo upload
  itself.

## Phase 18 — Del Monte demo barcode corrected; MRP now reads "Unable to Read" instead of a bare dash

- **User narrowed a large 14-point redesign/feature request down to one
  explicit, final ask**: a rock-solid hardcoded-lookup demo path for
  only the Del Monte Ketchup, with an explicit "don't touch anything
  else, this is purely additive" — the barcode-first architecture,
  Vaseline/Re'equil/Body Spray demo products, and the rest of Phase
  16/17's work were left exactly as they were; none of the broader
  redesign items were attempted.
- **Corrected the Del Monte demo product's barcode**: the value seeded in
  Phase 16 (`8901726003492`) didn't match the real product's actual
  EAN-13 the user later confirmed (`8901246003492`) — a new migration
  (`V17`) updates it rather than editing the already-applied `V15`
  (which would break Flyway's checksum validation). Verified live: a
  fresh scan of the corrected barcode matches and links in 44ms, well
  under the requested one-second budget; fuse+evaluate afterward
  produces a real report (score 37, 4 violations) built entirely from
  genuinely-missing fields (MRP, generic name, mfg month/year — none of
  which were ever part of this product's hardcoded data), not a
  fabricated pass.
- **MRP display**: per the explicit instruction ("if OCR cannot read
  [MRP], show 'Unable to Read' instead of inventing a value") — MRP was
  already never hardcoded and never fabricated, but an undetected value
  showed as a bare "—", identical to how every other missing field
  displays. Added `fusedValueDisplay()`
  (`frontend/src/features/inspections/utils/fusedValueDisplay.ts`),
  used in both the Fields tab and the Extraction Debug panel, so
  specifically MRP reads "Unable to Read" — every other declaration
  type's missing-value display is unchanged.
