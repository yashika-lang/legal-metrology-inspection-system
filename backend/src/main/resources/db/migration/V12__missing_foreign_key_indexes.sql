-- ============================================================================
-- Phase 4, Part 7: Performance review — foreign key columns that are
-- queried directly but had no supporting index. Found via the review, not
-- via a slow-query complaint, so these are preventative rather than fixes
-- for an observed regression.
-- ============================================================================

create index if not exists idx_compliance_reports_inspection on compliance_reports (inspection_id);
create index if not exists idx_chat_history_user on chat_history (user_id);
create index if not exists idx_product_categories_parent on product_categories (parent_category_id);
