-- ============================================================================
-- Phase 4, Part 4: Smart Report Generation.
--
-- compliance_reports.excel_path was speculative (Phase 1 named it before the
-- export format was decided) and has never been written to. It turns out
-- Phase 2's V3__ai_engine_schema.sql already added a docx_path column
-- ("AI report generator (Step 10): DOCX sits alongside the existing
-- pdf_path") anticipating exactly this feature — so no rename is needed,
-- docx_path already exists and is what ComplianceReport.docxPath maps to.
-- This migration only drops the now-confirmed-dead excel_path column.
-- ============================================================================

alter table compliance_reports drop column excel_path;
