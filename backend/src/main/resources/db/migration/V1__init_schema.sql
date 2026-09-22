-- ============================================================================
-- Legal Metrology AI Inspection System — initial schema
-- Target: Supabase PostgreSQL. Mirrors the JPA entities under
-- com.legalmetrology exactly (Hibernate runs with ddl-auto=validate, so this
-- file is the single source of truth for the schema, not an approximation).
-- ============================================================================

create extension if not exists "pgcrypto"; -- gen_random_uuid()

-- ============================================================================
-- IDENTITY & ACCESS
-- ============================================================================

create table roles (
    id          uuid primary key default gen_random_uuid(),
    name        varchar(30) not null unique,
    description varchar(255),
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create table users (
    id                uuid primary key default gen_random_uuid(),
    full_name         varchar(150) not null,
    email             varchar(255) not null unique,
    password_hash     varchar(255) not null,
    employee_code     varchar(50) unique,
    phone             varchar(20),
    preferred_locale  varchar(10) not null default 'en',
    is_active         boolean not null default true,
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now()
);

create table user_roles (
    user_id uuid not null references users (id) on delete cascade,
    role_id uuid not null references roles (id) on delete cascade,
    primary key (user_id, role_id)
);

create table refresh_tokens (
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid not null references users (id) on delete cascade,
    token      varchar(512) not null unique,
    expires_at timestamptz not null,
    revoked    boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_refresh_tokens_user on refresh_tokens (user_id);

create table password_reset_tokens (
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid not null references users (id) on delete cascade,
    token      varchar(255) not null unique,
    expires_at timestamptz not null,
    used       boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_password_reset_tokens_user on password_reset_tokens (user_id);

-- ============================================================================
-- PRODUCT DOMAIN
-- ============================================================================

create table product_categories (
    id                  uuid primary key default gen_random_uuid(),
    name                varchar(150) not null,
    parent_category_id  uuid references product_categories (id),
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

create table manufacturers (
    id         uuid primary key default gen_random_uuid(),
    name       varchar(255) not null,
    gstin      varchar(20),
    address    varchar(500),
    region     varchar(100),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table products (
    id              uuid primary key default gen_random_uuid(),
    name            varchar(255) not null,
    category_id     uuid references product_categories (id),
    manufacturer_id uuid references manufacturers (id),
    barcode         varchar(50) unique,
    default_unit    varchar(20),
    created_by      uuid references users (id),
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now()
);

create index idx_products_category on products (category_id);
create index idx_products_manufacturer on products (manufacturer_id);
create index idx_products_barcode on products (barcode);

-- ============================================================================
-- INSPECTION DOMAIN
-- ============================================================================

create table inspections (
    id                  uuid primary key default gen_random_uuid(),
    inspector_id        uuid not null references users (id),
    product_id          uuid references products (id),
    status              varchar(20) not null default 'DRAFT'
                        check (status in ('DRAFT','IN_PROGRESS','PENDING_REVIEW','COMPLETED','CLOSED')),
    location_lat        double precision,
    location_lng        double precision,
    compliance_score    numeric(5,2),
    fraud_risk          varchar(10) check (fraud_risk in ('LOW','MEDIUM','HIGH')),
    started_at          timestamptz not null default now(),
    completed_at        timestamptz,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

create index idx_inspections_inspector on inspections (inspector_id);
create index idx_inspections_product on inspections (product_id);
create index idx_inspections_open_status on inspections (status) where status <> 'CLOSED';

create table inspection_status_history (
    id            uuid primary key default gen_random_uuid(),
    inspection_id uuid not null references inspections (id) on delete cascade,
    from_status   varchar(20) check (from_status in ('DRAFT','IN_PROGRESS','PENDING_REVIEW','COMPLETED','CLOSED')),
    to_status     varchar(20) not null check (to_status in ('DRAFT','IN_PROGRESS','PENDING_REVIEW','COMPLETED','CLOSED')),
    changed_by    uuid references users (id),
    note          varchar(500),
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);

create index idx_status_history_inspection on inspection_status_history (inspection_id);

create table scans (
    id            uuid primary key default gen_random_uuid(),
    inspection_id uuid not null references inspections (id) on delete cascade,
    scan_type     varchar(10) not null check (scan_type in ('BARCODE','QR','MANUAL')),
    scan_value    varchar(255) not null,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);

create index idx_scans_inspection on scans (inspection_id);

-- ============================================================================
-- IMAGES & AI DETECTION DATA MODEL
-- (AI-populated columns exist now so the schema does not need to change when
--  the ocr/vision/rules modules are implemented in a later phase.)
-- ============================================================================

create table images (
    id               uuid primary key default gen_random_uuid(),
    inspection_id    uuid not null references inspections (id) on delete cascade,
    storage_path     varchar(500) not null,
    image_type       varchar(10) not null default 'OTHER' check (image_type in ('FRONT','BACK','SIDE','OTHER')),
    quality_score    numeric(5,2),
    quality_issues   jsonb not null default '{}'::jsonb,
    perceptual_hash  varchar(32),
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now()
);

create index idx_images_inspection on images (inspection_id);
create index idx_images_perceptual_hash on images (perceptual_hash);

create table ocr_results (
    id              uuid primary key default gen_random_uuid(),
    image_id        uuid not null references images (id) on delete cascade,
    provider        varchar(20) not null check (provider in ('GOOGLE_VISION','TESSERACT')),
    raw_text        text,
    corrected_text  text,
    confidence      numeric(5,4),
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now()
);

create index idx_ocr_results_image on ocr_results (image_id);

-- ============================================================================
-- RULES & COMPLIANCE DATA MODEL
-- (rule EVALUATION logic ships with the future `rules` module — this phase
--  only seeds the master rule data and the tables it evaluates into.)
-- ============================================================================

create table rules (
    id                uuid primary key default gen_random_uuid(),
    rule_code         varchar(50) not null unique,
    title             varchar(255) not null,
    description       text,
    legal_reference   varchar(255),
    category          varchar(100),
    default_severity  varchar(10) not null default 'MINOR' check (default_severity in ('CRITICAL','MAJOR','MINOR')),
    is_active         boolean not null default true,
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now()
);

create table violations (
    id              uuid primary key default gen_random_uuid(),
    inspection_id   uuid not null references inspections (id) on delete cascade,
    rule_id         uuid not null references rules (id),
    severity        varchar(10) not null check (severity in ('CRITICAL','MAJOR','MINOR')),
    description     text,
    ai_explanation  text,
    confidence      numeric(5,4),
    status          varchar(15) not null default 'OPEN' check (status in ('OPEN','ACKNOWLEDGED','RESOLVED')),
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now()
);

create index idx_violations_inspection on violations (inspection_id, status);
create index idx_violations_rule on violations (rule_id);

-- ============================================================================
-- AI ARTIFACTS DATA MODEL (populated once the `ai` module ships)
-- ============================================================================

create table ai_responses (
    id           uuid primary key default gen_random_uuid(),
    ref_type     varchar(20) not null check (ref_type in ('INSPECTION','VIOLATION','PRODUCT')),
    ref_id       uuid not null,
    prompt       text not null,
    response     text not null,
    model        varchar(100) not null,
    token_count  integer,
    latency_ms   integer,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

create index idx_ai_responses_ref on ai_responses (ref_type, ref_id);

create table chat_history (
    id             uuid primary key default gen_random_uuid(),
    user_id        uuid not null references users (id),
    inspection_id  uuid references inspections (id) on delete cascade,
    role           varchar(10) not null check (role in ('USER','ASSISTANT')),
    message        text not null,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now()
);

create index idx_chat_history_inspection on chat_history (inspection_id);

-- ============================================================================
-- REPORTING & OPS
-- ============================================================================

create table compliance_reports (
    id             uuid primary key default gen_random_uuid(),
    inspection_id  uuid not null references inspections (id) on delete cascade,
    report_number  varchar(50) not null unique,
    pdf_path       varchar(500),
    excel_path     varchar(500),
    generated_by   uuid references users (id),
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now()
);

create table audit_logs (
    id           uuid primary key default gen_random_uuid(),
    user_id      uuid references users (id),
    action       varchar(100) not null,
    entity_type  varchar(100) not null,
    entity_id    uuid,
    metadata     text,
    ip_address   varchar(45),
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

create index idx_audit_logs_entity on audit_logs (entity_type, entity_id);
create index idx_audit_logs_user on audit_logs (user_id);

create table settings (
    id          uuid primary key default gen_random_uuid(),
    key         varchar(150) not null unique,
    value       text not null,
    updated_by  uuid references users (id),
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

-- ============================================================================
-- updated_at maintenance trigger
-- ============================================================================

create or replace function set_updated_at()
returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

do $$
declare
    t text;
begin
    for t in
        select unnest(array[
            'roles','users','refresh_tokens','password_reset_tokens',
            'product_categories','manufacturers','products',
            'inspections','inspection_status_history','scans',
            'images','ocr_results','rules','violations',
            'ai_responses','chat_history','compliance_reports',
            'audit_logs','settings'
        ])
    loop
        execute format(
            'create trigger trg_%s_updated_at before update on %I
             for each row execute function set_updated_at();', t, t);
    end loop;
end $$;
