-- ============================================================================
-- Risk Index weights (analytics.risk.RiskIndexService) — configurable, not
-- hardcoded, same pattern as the compliance-scoring weights seeded in V7.
-- ============================================================================

insert into settings (id, key, value) values
    (gen_random_uuid(), 'risk.weight.violationRate', '0.4'),
    (gen_random_uuid(), 'risk.weight.criticalRatio', '0.3'),
    (gen_random_uuid(), 'risk.weight.complianceGap', '0.3'),
    (gen_random_uuid(), 'risk.referenceMaxViolationRate', '3.0')
on conflict (key) do nothing;
