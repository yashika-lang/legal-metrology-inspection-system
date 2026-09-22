-- ============================================================================
-- Wires the rule master data seeded in V2 up to the rule engine.
--
-- Simple presence/combination checks get a Spring Expression Language (SpEL)
-- snippet in `validation_expression`, evaluated against a context of
-- declarations extracted for the inspection (#declarations['MRP'].present,
-- etc. — see rules.engine.RuleEngineService for the exact context shape).
-- A rule with no expression is evaluated by a Java strategy registered
-- against its rule_code instead (rules.engine.strategy.*) — LM-FONTSIZE-001
-- and LM-USP-001 (unit sale price isn't a field this phase's OCR/Vision
-- extraction targets, so it has neither an expression nor a strategy yet
-- and is skipped by the engine, logged as not-yet-evaluable).
-- ============================================================================

update rules set validation_expression = '#declarations[''MRP''].present'
    where rule_code = 'LM-MRP-001';

update rules set validation_expression = '#declarations[''NET_QUANTITY''].present'
    where rule_code = 'LM-NETQTY-001';

update rules set validation_expression = '#declarations[''MFG_MONTH''].present and #declarations[''MFG_YEAR''].present'
    where rule_code = 'LM-MFGDATE-001';

update rules set validation_expression = '#declarations[''MANUFACTURER''].present and #declarations[''ADDRESS''].present'
    where rule_code = 'LM-MFRNAME-001';

update rules set validation_expression = '#declarations[''CUSTOMER_CARE''].present or #declarations[''EMAIL''].present'
    where rule_code = 'LM-CONSCARE-001';

update rules set validation_expression = '#declarations[''COUNTRY_OF_ORIGIN''].present'
    where rule_code = 'LM-COO-001';

update rules set validation_expression = '#declarations[''GENERIC_NAME''].present'
    where rule_code = 'LM-GENNAME-001';

-- LM-FONTSIZE-001 is evaluated by a Java strategy (FontReadabilityRuleValidator)
-- because it aggregates font analysis across every detection on the label
-- rather than checking a single field's presence. Its threshold is
-- data-driven via rule_parameters so it can be tuned without a redeploy.
insert into rule_parameters (id, rule_id, param_key, param_value)
select gen_random_uuid(), r.id, 'minReadabilityScore', '55'
from rules r
where r.rule_code = 'LM-FONTSIZE-001'
on conflict (rule_id, param_key) do nothing;
