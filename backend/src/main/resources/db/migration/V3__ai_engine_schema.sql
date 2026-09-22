-- ============================================================================
-- Phase 2 — AI Engine & Legal Metrology Compliance Engine schema additions.
-- Purely additive: new tables, and new nullable columns on existing tables.
-- Nothing here alters or drops an existing column, so every Phase 1
-- entity/query keeps working unmodified.
-- ============================================================================

create extension if not exists vector; -- pgvector, for semantic search embeddings

-- ----------------------------------------------------------------------------
-- Image preprocessing (Step 1): quality findings live alongside the existing
-- images.quality_score column (already present, was unused until now).
-- ----------------------------------------------------------------------------

alter table images
    add column if not exists quality_warnings   text,
    add column if not exists recommended_action varchar(30)
        check (recommended_action in ('NONE', 'RETAKE', 'ENHANCE'));

-- ----------------------------------------------------------------------------
-- Label detections (Steps 2 & 4): every declaration found on a label, whether
-- extracted from OCR text or located visually by Vision AI, plus the font
-- analysis (Step 6) computed for that specific detection.
-- ----------------------------------------------------------------------------

create table label_detections (
    id                  uuid primary key default gen_random_uuid(),
    image_id            uuid not null references images (id) on delete cascade,
    declaration_type    varchar(30) not null,
    detected_value      text,
    confidence          numeric(5,4),
    source              varchar(15) not null check (source in ('OCR_TEXT', 'VISION_AI')),
    bounding_box        text,
    is_present          boolean not null default false,
    label_section       varchar(10) check (label_section in ('FRONT', 'BACK', 'SIDE', 'UNKNOWN')),
    font_size_estimate  numeric(6,2),
    readability_score   numeric(5,2),
    contrast_score      numeric(5,2),
    font_issue          varchar(15) check (font_issue in ('NONE', 'TOO_SMALL', 'UNREADABLE', 'HIDDEN')),
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

create index idx_label_detections_image on label_detections (image_id);
create index idx_label_detections_type on label_detections (declaration_type);

create trigger trg_label_detections_updated_at before update on label_detections
    for each row execute function set_updated_at();

-- ----------------------------------------------------------------------------
-- Rule engine (Step 5): rules stay database-driven. Simple presence/threshold
-- rules carry a Spring Expression Language (SpEL) snippet evaluated against
-- the inspection's extracted declarations — new rules of this shape can be
-- added purely via data, no deployment required. Rules needing bespoke logic
-- (e.g. font readability) are matched by rule_code to a registered Java
-- strategy instead; validation_expression stays null for those.
-- ----------------------------------------------------------------------------

alter table rules
    add column if not exists validation_expression text;

create table rule_parameters (
    id          uuid primary key default gen_random_uuid(),
    rule_id     uuid not null references rules (id) on delete cascade,
    param_key   varchar(100) not null,
    param_value varchar(255) not null,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    unique (rule_id, param_key)
);

create trigger trg_rule_parameters_updated_at before update on rule_parameters
    for each row execute function set_updated_at();

-- ----------------------------------------------------------------------------
-- Violation engine (Step 8): each violation now carries the evidence needed
-- to render it on a report — where it was found and how to fix it.
-- ----------------------------------------------------------------------------

alter table violations
    add column if not exists bounding_box  text,
    add column if not exists suggested_fix text;

-- ----------------------------------------------------------------------------
-- Analytics (Step 12): region is captured per-inspection so region-level
-- rollups are possible without a full jurisdiction/office hierarchy.
-- ----------------------------------------------------------------------------

alter table inspections
    add column if not exists region varchar(100);

create index if not exists idx_inspections_region on inspections (region);

-- ----------------------------------------------------------------------------
-- AI report generator (Step 10): DOCX sits alongside the existing pdf_path.
-- ----------------------------------------------------------------------------

alter table compliance_reports
    add column if not exists docx_path varchar(500);

-- ----------------------------------------------------------------------------
-- Semantic search (Step 11): embeddings are managed outside JPA/Hibernate
-- (via JdbcTemplate + native pgvector SQL) since the `vector` type and its
-- `<=>` distance operator have no first-class ORM mapping in this stack —
-- see ai.embedding.EmbeddingRepository.
-- ----------------------------------------------------------------------------

create table embeddings (
    id         uuid primary key default gen_random_uuid(),
    ref_type   varchar(20) not null check (ref_type in ('INSPECTION', 'PRODUCT', 'VIOLATION')),
    ref_id     uuid not null,
    content    text not null,
    embedding  vector(768) not null,
    created_at timestamptz not null default now()
);

create index idx_embeddings_ref on embeddings (ref_type, ref_id);
create index idx_embeddings_vector on embeddings using ivfflat (embedding vector_cosine_ops) with (lists = 100);
