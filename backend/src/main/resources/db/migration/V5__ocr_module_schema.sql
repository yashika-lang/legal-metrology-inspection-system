-- ============================================================================
-- OCR module (Steps 2-3): structured, multilingual, hierarchical OCR with
-- provider audit history and cross-source confidence fusion.
-- Purely additive — new nullable columns and two new tables.
-- ============================================================================

-- Which OCR provider/language produced a given label_detections row (only
-- set when source = 'OCR_TEXT'; NULL for VISION_AI-sourced rows).
alter table label_detections
    add column if not exists ocr_provider varchar(30),
    add column if not exists language     varchar(10);

-- Full paragraph/line/word hierarchy with coordinates (Step: "OCR should
-- preserve text hierarchy"), stored as JSON text — see ocr.model.OcrExtractionResult.
-- Kept as `text`, not `jsonb`, so it binds as a plain JDBC string (no
-- Postgres-side type coercion, avoiding the class of bug documented on
-- BaseEntity's id column).
alter table ocr_results
    add column if not exists structured_hierarchy   text,
    add column if not exists detected_language       varchar(10),
    add column if not exists correction_confidence   numeric(5,4);

-- One row per (inspection, declaration_type): the current best-understood
-- value after merging every OCR run across all of the inspection's images
-- (front/back/side) and fusing that against the Vision AI signal. Raw
-- per-source values are never discarded — both sides of a disagreement are
-- kept on this same row for transparency.
create table fused_declarations (
    id                        uuid primary key default gen_random_uuid(),
    inspection_id             uuid not null references inspections (id) on delete cascade,
    declaration_type          varchar(30) not null,
    fused_value               text,
    fused_confidence          numeric(5,4),
    is_present                boolean not null default false,
    ocr_value                 text,
    ocr_confidence            numeric(5,4),
    ocr_source_detection_id   uuid,
    vision_value              text,
    vision_confidence         numeric(5,4),
    vision_source_detection_id uuid,
    agreement                 boolean,
    created_at                timestamptz not null default now(),
    updated_at                timestamptz not null default now(),
    unique (inspection_id, declaration_type)
);

create index idx_fused_declarations_inspection on fused_declarations (inspection_id);

create trigger trg_fused_declarations_updated_at before update on fused_declarations
    for each row execute function set_updated_at();
