-- ============================================================================
-- Rules module (Step 5): a fully database-driven rule engine.
--
-- Design: every rule declares a `validation_type` (one of a fixed set of
-- generic, reusable validator STRATEGIES implemented in Java — see
-- rules.engine.validator) and a `validation_expression` (a JSON parameter
-- blob interpreted by that strategy, e.g. {"field":"MRP","pattern":"..."}
-- for REGEX). Adding a new simple rule is a pure data change — insert a row
-- referencing an existing validation_type; no code changes, no redeploy.
-- Only genuinely novel *composite* logic needs a new Java CUSTOM strategy,
-- registered by name and referenced from validation_expression.
--
-- Rule versioning: rows are never mutated in place once used by an
-- evaluation. "Updating" a rule creates a new row sharing rule_code with
-- version+1 and deactivates the previous version — so a Violation's FK to
-- the specific Rule row it was evaluated against always resolves to the
-- exact content that was active at the time, even after the rule changes
-- later. See rules.service.RuleService.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- rules: add the fields the spec requires, rename default_severity -> severity
-- for clarity now that "default" no longer means anything (there's no
-- longer a separate per-inspection override), and replace the plain
-- unique(rule_code) with unique(rule_code, version) plus a partial unique
-- index guaranteeing at most one ACTIVE version per rule_code.
-- ----------------------------------------------------------------------------

alter table rules rename column default_severity to severity;

alter table rules drop constraint if exists rules_rule_code_key;

alter table rules
    add column if not exists validation_type    varchar(20),
    add column if not exists mandatory          boolean not null default true,
    add column if not exists suggestion         text,
    add column if not exists penalty_reference  text,
    add column if not exists version            integer not null default 1,
    add column if not exists effective_date     date not null default current_date;

alter table rules add constraint uq_rules_code_version unique (rule_code, version);

create unique index if not exists uq_rules_one_active_version_per_code
    on rules (rule_code) where is_active = true;

-- ----------------------------------------------------------------------------
-- violations: the explicit field/actual/expected/legal-reference columns
-- the Violation Engine requirement lists. legal_reference is a denormalized
-- snapshot of rule.legal_reference at evaluation time — purely for cheap
-- API reads; the FK to the specific rule VERSION is what actually
-- guarantees historical accuracy (see the versioning note above).
-- ----------------------------------------------------------------------------

alter table violations
    add column if not exists field          varchar(30),
    add column if not exists actual_value   text,
    add column if not exists expected_value text,
    add column if not exists legal_reference text;

-- ----------------------------------------------------------------------------
-- fused_declarations: carry forward the font-analysis metrics computed on
-- whichever detection (OCR or Vision AI) the fusion step selected as the
-- winning value — FONT_SIZE/READABILITY/PLACEMENT rule types read these
-- directly off the fused view, so the rule engine needs only one input
-- source (see rules.model.DeclarationSnapshot) rather than reaching back
-- into per-image label_detections itself.
-- ----------------------------------------------------------------------------

alter table fused_declarations
    add column if not exists font_size_estimate numeric(6,2),
    add column if not exists readability_score   numeric(5,2),
    add column if not exists contrast_score       numeric(5,2),
    add column if not exists label_section        varchar(10),
    add column if not exists bounding_box         text;
