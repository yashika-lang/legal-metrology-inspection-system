-- ============================================================================
-- Phase 4, Part 1 & 3: Decision Trace + Evidence Engine.
--
-- decision_trace_steps: one row per pipeline stage executed for an
-- inspection (image quality, OCR, vision, fusion, rule evaluation,
-- scoring, evidence, report). Referenced rule/evidence/image ids are
-- deliberately plain UUID columns with no FK constraint — this is an
-- append-only audit/traceability log (same pattern as audit_logs), and a
-- loose reference here must never block deleting/evolving the thing it
-- points at.
--
-- evidence: one legally-traceable record per violation — the original and
-- annotated image, every confidence signal that fed the decision, and a
-- SHA-256 hash of its own content for future tamper detection / digital
-- signature verification without any architecture change later.
-- ============================================================================

create table decision_trace_steps (
    id                    uuid primary key default gen_random_uuid(),
    inspection_id         uuid not null references inspections (id) on delete cascade,
    step_name             varchar(30) not null,
    module                varchar(100) not null,
    input_summary         text,
    output_summary        text,
    confidence            numeric(5,4),
    execution_time_ms     bigint not null default 0,
    started_at            timestamptz not null,
    status                varchar(15) not null check (status in ('SUCCESS', 'FAILED', 'SKIPPED')),
    reason                text,
    referenced_rule_id    uuid,
    referenced_evidence_id uuid,
    referenced_image_id   uuid,
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now()
);

create index idx_decision_trace_inspection on decision_trace_steps (inspection_id, created_at);

create trigger trg_decision_trace_steps_updated_at before update on decision_trace_steps
    for each row execute function set_updated_at();

create table evidence (
    id                    uuid primary key default gen_random_uuid(),
    sha256_hash           varchar(64) not null,
    inspection_id         uuid not null references inspections (id) on delete cascade,
    image_id              uuid references images (id) on delete set null,
    violation_id          uuid not null references violations (id) on delete cascade,
    original_image_path   varchar(500),
    annotated_image_path  varchar(500),
    bounding_box          text,
    ocr_text              text,
    normalized_value      text,
    vision_confidence     numeric(5,4),
    ocr_confidence        numeric(5,4),
    fused_confidence      numeric(5,4),
    expected_value        text,
    actual_value          text,
    reason                text,
    suggested_fix         text,
    legal_rule_reference  text,
    severity              varchar(10),
    metadata              text,
    is_immutable          boolean not null default false,
    signature             text,
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now()
);

create index idx_evidence_inspection on evidence (inspection_id);
create index idx_evidence_violation on evidence (violation_id);
create index idx_evidence_image on evidence (image_id);
create unique index uq_evidence_hash on evidence (sha256_hash);

create trigger trg_evidence_updated_at before update on evidence
    for each row execute function set_updated_at();
