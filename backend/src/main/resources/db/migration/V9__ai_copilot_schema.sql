-- ============================================================================
-- AI Compliance Copilot (Phase 3, Part 1-2): extends the existing
-- chat_history/ai_responses data model from Phase 1 rather than replacing
-- it. Purely additive.
-- ============================================================================

alter table chat_history
    add column if not exists provider  varchar(30),
    add column if not exists report_id uuid references compliance_reports (id);

create index if not exists idx_chat_history_report on chat_history (report_id);
