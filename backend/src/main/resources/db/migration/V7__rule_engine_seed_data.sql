-- ============================================================================
-- Re-seeds the Legal Metrology rule set onto the new validation_type /
-- validation_expression schema introduced in V6. Supersedes the V2/V4 seed
-- data (which predates the versioned rule engine) via UPDATE, not by
-- editing those already-shipped migrations — Flyway migrations are
-- immutable once applied. Every one of the 12 validation types is
-- exercised by at least one real rule below.
-- ============================================================================

-- ---- Presence checks (FIELD_EXISTS) ----

update rules set
    validation_type = 'FIELD_EXISTS',
    validation_expression = '{"field":"MRP"}',
    mandatory = true,
    suggestion = 'Add the Maximum Retail Price (MRP), inclusive of all taxes, prominently on the package.',
    penalty_reference = 'Legal Metrology Act, 2009 — Section 36: fine up to Rs 25,000 for a first offence, imprisonment possible on repeat offence.'
where rule_code = 'LM-MRP-001';

update rules set
    validation_type = 'FIELD_EXISTS',
    validation_expression = '{"field":"NET_QUANTITY"}',
    mandatory = true,
    suggestion = 'Add the net quantity of the commodity in standard units (e.g. grams, millilitres).',
    penalty_reference = 'Legal Metrology Act, 2009 — Section 36.'
where rule_code = 'LM-NETQTY-001';

update rules set
    validation_type = 'FIELD_EXISTS',
    validation_expression = '{"field":"MANUFACTURER"}',
    mandatory = true,
    suggestion = 'Add the manufacturer''s name.',
    penalty_reference = 'Legal Metrology Act, 2009 — Section 36.'
where rule_code = 'LM-MFRNAME-001';

update rules set
    validation_type = 'FIELD_EXISTS',
    validation_expression = '{"field":"CUSTOMER_CARE"}',
    mandatory = false,
    severity = 'MINOR',
    suggestion = 'Add a consumer-care phone number, address, or email for complaints.'
where rule_code = 'LM-CONSCARE-001';

update rules set
    validation_type = 'FIELD_EXISTS',
    validation_expression = '{"field":"GENERIC_NAME"}',
    mandatory = true,
    suggestion = 'Add the common/generic name of the commodity.'
where rule_code = 'LM-GENNAME-001';

-- ---- LM-MFGDATE-001 is split into two granular checks — deactivate the combined original. ----
update rules set is_active = false where rule_code = 'LM-MFGDATE-001';

insert into rules (id, rule_code, title, description, legal_reference, category, severity,
                    validation_type, validation_expression, mandatory, suggestion, penalty_reference, version, effective_date, is_active)
values
    (gen_random_uuid(), 'LM-MFGMONTH-001', 'Month of manufacture present',
        'The package must declare the month of manufacture/packing.', 'LMPC Rule 6(1)(f)', 'MANDATORY_DECLARATION', 'MAJOR',
        'FIELD_EXISTS', '{"field":"MFG_MONTH"}', true, 'Add the month of manufacture/packing.', null, 1, current_date, true),
    (gen_random_uuid(), 'LM-MFGYEAR-001', 'Year of manufacture present',
        'The package must declare the year of manufacture/packing.', 'LMPC Rule 6(1)(f)', 'MANDATORY_DECLARATION', 'MAJOR',
        'FIELD_EXISTS', '{"field":"MFG_YEAR"}', true, 'Add the year of manufacture/packing.', null, 1, current_date, true)
on conflict (rule_code, version) do nothing;

-- ---- FIELD_NOT_EMPTY: detected but blank is still a failure ----
-- LM-LICENSE-001 has no V2 seed row to UPDATE (FSSAI licensing wasn't one of
-- the original 9 rules) — insert it fresh.

insert into rules (id, rule_code, title, description, legal_reference, category, severity,
                    validation_type, validation_expression, mandatory, suggestion, penalty_reference, version, effective_date, is_active)
values
    (gen_random_uuid(), 'LM-LICENSE-001', 'FSSAI license number present and legible',
        'The package must declare a legible FSSAI license number.', 'FSS (Licensing and Registration) Regulations, 2011',
        'MANDATORY_DECLARATION', 'CRITICAL',
        'FIELD_NOT_EMPTY', '{"field":"LICENSE_NUMBER"}', true, 'Add a legible FSSAI license number.',
        'Food Safety and Standards Act, 2006 — Section 63: imprisonment up to 6 months and/or fine up to Rs 5 lakh for operating without a valid license.',
        1, current_date, true)
on conflict (rule_code, version) do nothing;

-- ---- REGEX / MIN_LENGTH / MAX_LENGTH / ENUM / DATE / NUMERIC / FONT_SIZE / READABILITY / PLACEMENT / CUSTOM ----

update rules set
    validation_type = 'ENUM',
    validation_expression = '{"field":"COUNTRY_OF_ORIGIN","allowedValues":["India","China","USA","United States","Germany","Japan","Vietnam","Bangladesh","Sri Lanka","Thailand","Malaysia","Indonesia","UAE","United Arab Emirates","United Kingdom","France","Italy"]}',
    mandatory = false,
    suggestion = 'Ensure the declared country of origin is a recognizable country name.'
where rule_code = 'LM-COO-001';

-- LM-FONTSIZE-001 is repurposed as the READABILITY check on the highest-stakes field (MRP).
update rules set
    validation_type = 'READABILITY',
    validation_expression = '{"field":"MRP","minScore":55}',
    mandatory = false,
    suggestion = 'Reprint the MRP with higher contrast and a clearer font so it is legible at a glance.'
where rule_code = 'LM-FONTSIZE-001';

-- LM-USP-001 (unit sale price) isn't a field this phase's OCR/Vision extraction targets — deactivate honestly rather than leave it unevaluable.
update rules set is_active = false where rule_code = 'LM-USP-001';

insert into rules (id, rule_code, title, description, legal_reference, category, severity,
                    validation_type, validation_expression, mandatory, suggestion, penalty_reference, version, effective_date, is_active)
values
    (gen_random_uuid(), 'LM-MRP-FORMAT-001', 'MRP printed in a recognizable currency format',
        'The MRP value should be printed as a clear ₹<amount> figure.', 'LMPC Rule 6(1)(e)', 'READABILITY', 'MAJOR',
        'REGEX', '{"field":"MRP","pattern":"^₹\\d+(\\.\\d{2})?$"}', false,
        'Print the MRP as ₹<amount>, e.g. ₹120 or ₹120.50.', null, 1, current_date, true),

    (gen_random_uuid(), 'LM-MRP-POSITIVE-001', 'MRP is a valid positive amount',
        'The MRP value should parse as a positive number.', 'LMPC Rule 6(1)(e)', 'MANDATORY_DECLARATION', 'MINOR',
        'NUMERIC', '{"field":"MRP","min":1}', false,
        'Verify the printed MRP is a valid positive amount.', null, 1, current_date, true),

    (gen_random_uuid(), 'LM-ADDRESS-001', 'Manufacturer address is complete enough to be useful',
        'The declared address should be long enough to plausibly be a full postal address, not a fragment.',
        'LMPC Rule 6(1)(a)', 'MANDATORY_DECLARATION', 'MAJOR',
        'MIN_LENGTH', '{"field":"ADDRESS","minLength":10}', true,
        'Add the complete manufacturer/packer address, including city and PIN code.', null, 1, current_date, true),

    (gen_random_uuid(), 'LM-EMAIL-FORMAT-001', 'Consumer care email is a valid email format',
        'When a consumer-care email is declared, it should be a well-formed email address.',
        'LMPC Rule 6(1)(a)(ii)', 'MANDATORY_DECLARATION', 'MINOR',
        'REGEX', '{"field":"EMAIL","pattern":"^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"}', false,
        'Correct the consumer-care email address format.', null, 1, current_date, true),

    (gen_random_uuid(), 'LM-MFGDATE-NOTFUTURE-001', 'Manufacturing date is not in the future',
        'The declared manufacturing month/year should not be later than the current month.',
        'LMPC Rule 6(1)(f)', 'MANDATORY_DECLARATION', 'MINOR',
        'DATE', '{"field":"MFG_MONTH","format":"yyyy-MM","notFuture":true}', false,
        'Correct the manufacturing date — it appears to be in the future.', null, 1, current_date, true),

    (gen_random_uuid(), 'LM-NETQTY-FONT-001', 'Net quantity printed at a readable size',
        'The net quantity declaration''s text height, relative to the image, should meet a minimum threshold.',
        'LMPC Second Schedule', 'READABILITY', 'MAJOR',
        'FONT_SIZE', '{"field":"NET_QUANTITY","minSizePercent":1.2}', false,
        'Increase the font size of the net quantity declaration.', null, 1, current_date, true),

    (gen_random_uuid(), 'LM-PLACEMENT-001', 'MRP appears on the front (principal display) panel',
        'The Maximum Retail Price must be declared on the principal display panel, not only on a side/back panel.',
        'LMPC Rule 6', 'MANDATORY_DECLARATION', 'MAJOR',
        'PLACEMENT', '{"field":"MRP","allowedSections":["FRONT"]}', false,
        'Move or duplicate the MRP declaration onto the front (principal display) panel.', null, 1, current_date, true),

    (gen_random_uuid(), 'LM-PRODUCTNAME-LENGTH-001', 'Product name is a plausible length',
        'An excessively long detected product name usually indicates OCR noise rather than a real product name.',
        'LMPC Rule 6(1)(b)', 'MANDATORY_DECLARATION', 'MINOR',
        'MAX_LENGTH', '{"field":"PRODUCT_NAME","maxLength":150}', false,
        'Verify the detected product name — it may include OCR noise.', null, 1, current_date, true),

    (gen_random_uuid(), 'LM-COO-CONDITIONAL-001', 'Imported products must declare country of origin',
        'If an importer is declared, the country of origin becomes a mandatory declaration for that package.',
        'LMPC Rule 6(1)(a)(iii)', 'MANDATORY_DECLARATION', 'CRITICAL',
        'CUSTOM', '{"strategy":"IMPORTER_REQUIRES_ORIGIN"}', true,
        'This product declares an importer, so the country of origin must also be declared.',
        'Legal Metrology Act, 2009 — Section 36.', 1, current_date, true)
on conflict (rule_code, version) do nothing;

-- ============================================================================
-- Compliance scoring weights (Step 7): configurable, not hardcoded — see
-- rules.scoring.ComplianceScoreService, which reads these via the existing
-- generic Settings store rather than a bespoke table.
-- ============================================================================

insert into settings (id, key, value) values
    (gen_random_uuid(), 'scoring.severity.critical.weight', '-25'),
    (gen_random_uuid(), 'scoring.severity.major.weight', '-10'),
    (gen_random_uuid(), 'scoring.severity.minor.weight', '-3'),
    (gen_random_uuid(), 'scoring.imageQuality.weight', '5'),
    (gen_random_uuid(), 'scoring.ocrConfidence.weight', '5'),
    (gen_random_uuid(), 'scoring.fontReadability.weight', '5'),
    (gen_random_uuid(), 'scoring.missingFields.weight', '2')
on conflict (key) do nothing;
