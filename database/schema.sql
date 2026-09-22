-- ============================================================================
-- Legal Metrology AI Inspection System — Database Schema
-- Target: Supabase PostgreSQL
-- Auth: Supabase Auth manages auth.users; `profiles` extends it 1:1.
-- Apply as a Flyway migration (V1__init.sql) or directly via Supabase SQL editor.
-- ============================================================================

create extension if not exists "uuid-ossp";
create extension if not exists "pgcrypto";
create extension if not exists vector;               -- pgvector, for semantic search

-- ============================================================================
-- IDENTITY & ACCESS
-- ============================================================================

create table offices (
    id              uuid primary key default gen_random_uuid(),
    name            text not null,
    region          text not null,
    state           text not null,
    jurisdiction_code text not null unique,
    created_at      timestamptz not null default now()
);

-- Extends Supabase's auth.users (1:1). id == auth.users.id.
create table profiles (
    id                  uuid primary key references auth.users (id) on delete cascade,
    full_name           text not null,
    employee_code       text unique,
    office_id           uuid references offices (id),
    phone               text,
    preferred_locale    text not null default 'en',
    is_active           boolean not null default true,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

create table roles (
    id              uuid primary key default gen_random_uuid(),
    name            text not null unique,             -- INSPECTOR, SENIOR_INSPECTOR, OFFICE_ADMIN, SYSTEM_ADMIN
    description     text
);

create table permissions (
    id              uuid primary key default gen_random_uuid(),
    code            text not null unique,              -- e.g. INSPECTION_APPROVE, RULE_EDIT
    description     text
);

create table role_permissions (
    role_id         uuid not null references roles (id) on delete cascade,
    permission_id   uuid not null references permissions (id) on delete cascade,
    primary key (role_id, permission_id)
);

create table user_roles (
    user_id         uuid not null references profiles (id) on delete cascade,
    role_id         uuid not null references roles (id) on delete cascade,
    primary key (user_id, role_id)
);

-- ============================================================================
-- PRODUCT DOMAIN
-- ============================================================================

create table manufacturers (
    id              uuid primary key default gen_random_uuid(),
    name            text not null,
    gstin           text,
    address         text,
    region          text,
    created_at      timestamptz not null default now()
);

create table product_categories (
    id                  uuid primary key default gen_random_uuid(),
    name                text not null,
    parent_category_id  uuid references product_categories (id),
    created_at          timestamptz not null default now()
);

create table products (
    id              uuid primary key default gen_random_uuid(),
    name            text not null,
    category_id     uuid references product_categories (id),
    manufacturer_id uuid references manufacturers (id),
    barcode         text unique,
    default_unit    text,                              -- g, kg, ml, l, count
    created_by      uuid references profiles (id),
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now()
);

create index idx_products_category on products (category_id);
create index idx_products_manufacturer on products (manufacturer_id);

-- Powers "AI Barcode Intelligence": every scan of an unrecognized/known
-- barcode is logged here for lookup-frequency and history.
create table barcode_lookups (
    id              uuid primary key default gen_random_uuid(),
    barcode         text not null,
    product_id      uuid references products (id),
    lookup_count    integer not null default 1,
    last_seen_at    timestamptz not null default now()
);

create index idx_barcode_lookups_barcode on barcode_lookups (barcode);

-- ============================================================================
-- INSPECTION DOMAIN
-- ============================================================================

create table inspections (
    id                  uuid primary key default gen_random_uuid(),
    inspector_id        uuid not null references profiles (id),
    office_id           uuid not null references offices (id),
    product_id          uuid references products (id),      -- nullable: unknown at scan time
    status              text not null default 'DRAFT'
                        check (status in ('DRAFT','IN_PROGRESS','PENDING_REVIEW','COMPLETED','CLOSED')),
    location_lat        double precision,
    location_lng        double precision,
    compliance_score    numeric(5,2),                        -- 0.00–100.00, set after pipeline runs
    fraud_risk          text check (fraud_risk in ('LOW','MEDIUM','HIGH')),
    started_at          timestamptz not null default now(),
    completed_at        timestamptz,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

create index idx_inspections_inspector on inspections (inspector_id);
create index idx_inspections_office on inspections (office_id);
create index idx_inspections_product on inspections (product_id);
create index idx_inspections_open_status on inspections (status) where status <> 'CLOSED';

create table inspection_status_history (
    id              uuid primary key default gen_random_uuid(),
    inspection_id   uuid not null references inspections (id) on delete cascade,
    from_status     text,
    to_status       text not null,
    changed_by      uuid references profiles (id),
    changed_at      timestamptz not null default now(),
    note            text
);

create index idx_status_history_inspection on inspection_status_history (inspection_id);

create table scans (
    id              uuid primary key default gen_random_uuid(),
    inspection_id   uuid not null references inspections (id) on delete cascade,
    scan_type       text not null check (scan_type in ('BARCODE','QR','MANUAL')),
    scan_value      text not null,
    scanned_at      timestamptz not null default now()
);

create index idx_scans_inspection on scans (inspection_id);

-- ============================================================================
-- IMAGES & AI DETECTIONS
-- ============================================================================

create table images (
    id                  uuid primary key default gen_random_uuid(),
    inspection_id       uuid not null references inspections (id) on delete cascade,
    storage_path        text not null,                        -- Supabase Storage object path
    image_type          text not null default 'OTHER'
                        check (image_type in ('FRONT','BACK','SIDE','OTHER')),
    quality_score       numeric(5,2),
    quality_issues      jsonb not null default '{}'::jsonb,    -- {blurry, dark, cropped, rotated, glare}
    perceptual_hash      text,                                 -- for duplicate detection
    uploaded_at          timestamptz not null default now()
);

create index idx_images_inspection on images (inspection_id);
create index idx_images_perceptual_hash on images (perceptual_hash);

create table ocr_results (
    id              uuid primary key default gen_random_uuid(),
    image_id        uuid not null references images (id) on delete cascade,
    provider        text not null check (provider in ('GOOGLE_VISION','TESSERACT')),
    raw_text        text,
    corrected_text  text,
    confidence      numeric(5,4),
    created_at      timestamptz not null default now()
);

create index idx_ocr_results_image on ocr_results (image_id);

create table vision_detections (
    id                  uuid primary key default gen_random_uuid(),
    image_id            uuid not null references images (id) on delete cascade,
    declaration_type    text not null,                          -- MRP, NET_QUANTITY, MFG_DATE, EXPIRY_DATE,
                                                                  -- FSSAI, MANUFACTURER_NAME, CONSUMER_CARE,
                                                                  -- COUNTRY_OF_ORIGIN, UNIT_SALE_PRICE, ...
    detected_value      text,
    confidence          numeric(5,4),
    bounding_box        jsonb,                                   -- {x, y, w, h}
    is_present          boolean not null default false,
    font_size_estimate  numeric(6,2),
    readability_score   numeric(5,2),
    created_at          timestamptz not null default now()
);

create index idx_vision_detections_image on vision_detections (image_id);
create index idx_vision_detections_type on vision_detections (declaration_type);

-- ============================================================================
-- RULES & COMPLIANCE
-- ============================================================================

create table rules (
    id                  uuid primary key default gen_random_uuid(),
    rule_code           text not null unique,
    title               text not null,
    description         text,
    legal_reference      text,                                   -- e.g. "LMPC Rule 6(1)(f)"
    category            text,
    default_severity    text not null default 'MINOR'
                        check (default_severity in ('CRITICAL','MAJOR','MINOR')),
    is_active           boolean not null default true,
    created_at          timestamptz not null default now()
);

create table rule_parameters (
    id              uuid primary key default gen_random_uuid(),
    rule_id         uuid not null references rules (id) on delete cascade,
    param_key       text not null,
    param_value     text not null,
    unique (rule_id, param_key)
);

create table violations (
    id              uuid primary key default gen_random_uuid(),
    inspection_id   uuid not null references inspections (id) on delete cascade,
    rule_id         uuid not null references rules (id),
    severity        text not null check (severity in ('CRITICAL','MAJOR','MINOR')),
    description     text,
    ai_explanation  text,
    confidence      numeric(5,4),
    status          text not null default 'OPEN' check (status in ('OPEN','ACKNOWLEDGED','RESOLVED')),
    created_at      timestamptz not null default now()
);

create index idx_violations_inspection on violations (inspection_id, status);
create index idx_violations_rule on violations (rule_id);

-- ============================================================================
-- AI ARTIFACTS
-- ============================================================================

-- Full audit trail of every LLM call — cost tracking + reproducibility.
create table ai_explanations (
    id              uuid primary key default gen_random_uuid(),
    ref_type        text not null check (ref_type in ('VIOLATION','INSPECTION')),
    ref_id          uuid not null,
    prompt          text not null,
    response        text not null,
    model           text not null,
    token_count     integer,
    latency_ms      integer,
    created_at      timestamptz not null default now()
);

create index idx_ai_explanations_ref on ai_explanations (ref_type, ref_id);

-- pgvector embeddings for semantic search ("products with missing MRP", etc.)
create table embeddings (
    id              uuid primary key default gen_random_uuid(),
    ref_type        text not null check (ref_type in ('INSPECTION','PRODUCT','VIOLATION')),
    ref_id          uuid not null,
    content         text not null,
    embedding       vector(768) not null,
    created_at      timestamptz not null default now()
);

create index idx_embeddings_ref on embeddings (ref_type, ref_id);
create index idx_embeddings_vector on embeddings using ivfflat (embedding vector_cosine_ops) with (lists = 100);

create table chat_logs (
    id              uuid primary key default gen_random_uuid(),
    user_id         uuid not null references profiles (id),
    inspection_id   uuid references inspections (id) on delete cascade,
    role            text not null check (role in ('USER','ASSISTANT')),
    message         text not null,
    created_at      timestamptz not null default now()
);

create index idx_chat_logs_inspection on chat_logs (inspection_id);

-- ============================================================================
-- REPORTING & OPS
-- ============================================================================

create table compliance_reports (
    id              uuid primary key default gen_random_uuid(),
    inspection_id   uuid not null references inspections (id) on delete cascade,
    report_number   text not null unique,
    pdf_path        text,
    excel_path      text,
    generated_by    uuid references profiles (id),
    generated_at    timestamptz not null default now()
);

create table audit_logs (
    id              uuid primary key default gen_random_uuid(),
    user_id         uuid references profiles (id),
    action          text not null,
    entity_type     text not null,
    entity_id       uuid,
    metadata        jsonb not null default '{}'::jsonb,
    ip_address      inet,
    created_at      timestamptz not null default now()
);

create index idx_audit_logs_entity on audit_logs (entity_type, entity_id);
create index idx_audit_logs_metadata on audit_logs using gin (metadata);

create table settings (
    id              uuid primary key default gen_random_uuid(),
    key             text not null unique,
    value           jsonb not null,
    updated_by      uuid references profiles (id),
    updated_at      timestamptz not null default now()
);

create index idx_settings_value on settings using gin (value);

-- ============================================================================
-- updated_at maintenance trigger (applied to tables that carry updated_at)
-- ============================================================================

create or replace function set_updated_at()
returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

create trigger trg_profiles_updated_at before update on profiles
    for each row execute function set_updated_at();
create trigger trg_products_updated_at before update on products
    for each row execute function set_updated_at();
create trigger trg_inspections_updated_at before update on inspections
    for each row execute function set_updated_at();
