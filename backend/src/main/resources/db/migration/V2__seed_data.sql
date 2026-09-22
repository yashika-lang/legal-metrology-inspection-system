-- ============================================================================
-- Seed data: roles, a default admin account, and the Legal Metrology
-- (Packaged Commodities) Rules master data the future rule engine evaluates
-- against. Safe to re-run (guarded with ON CONFLICT DO NOTHING).
-- ============================================================================

create extension if not exists pgcrypto; -- crypt()/gen_salt() for the seed admin password

insert into roles (id, name, description) values
    (gen_random_uuid(), 'ADMIN',          'Full system access — user management, rule configuration, settings'),
    (gen_random_uuid(), 'SENIOR_OFFICER', 'Reviews and approves inspections, manages products, views analytics'),
    (gen_random_uuid(), 'INSPECTOR',      'Performs field inspections — scans, uploads, and submits for review')
on conflict (name) do nothing;

-- Default administrator account for first login. Password: Admin@123
-- CHANGE THIS IMMEDIATELY after first login in any non-local environment —
-- it exists only so a freshly migrated database has one way in.
insert into users (id, full_name, email, password_hash, employee_code, preferred_locale, is_active)
values (
    gen_random_uuid(),
    'System Administrator',
    'admin@legalmetrology.gov.in',
    crypt('Admin@123', gen_salt('bf', 12)),
    'ADMIN-001',
    'en',
    true
)
on conflict (email) do nothing;

insert into user_roles (user_id, role_id)
select u.id, r.id
from users u, roles r
where u.email = 'admin@legalmetrology.gov.in'
  and r.name = 'ADMIN'
on conflict do nothing;

-- Legal Metrology (Packaged Commodities) Rules, 2011 — mandatory declarations
-- most commonly checked during a packaged-commodity inspection. This is
-- master data for the future rule engine; evaluation logic is not part of
-- this phase.
insert into rules (id, rule_code, title, description, legal_reference, category, default_severity, is_active) values
    (gen_random_uuid(), 'LM-MRP-001', 'Maximum Retail Price (MRP) declaration',
        'The package must declare the maximum retail price inclusive of all taxes, prefixed with "MRP" and the applicable currency symbol.',
        'LMPC Rule 6(1)(e)', 'MANDATORY_DECLARATION', 'CRITICAL', true),
    (gen_random_uuid(), 'LM-NETQTY-001', 'Net quantity declaration',
        'The package must declare the net quantity of the commodity in standard units (weight, volume, or number).',
        'LMPC Rule 6(1)(c)', 'MANDATORY_DECLARATION', 'CRITICAL', true),
    (gen_random_uuid(), 'LM-MFGDATE-001', 'Month and year of manufacture/import',
        'The package must declare the month and year in which the commodity was manufactured, packed, or imported.',
        'LMPC Rule 6(1)(f)', 'MANDATORY_DECLARATION', 'MAJOR', true),
    (gen_random_uuid(), 'LM-MFRNAME-001', 'Manufacturer/packer/importer name and address',
        'The package must declare the name and complete address of the manufacturer, packer, or importer.',
        'LMPC Rule 6(1)(a)', 'MANDATORY_DECLARATION', 'CRITICAL', true),
    (gen_random_uuid(), 'LM-CONSCARE-001', 'Consumer care details',
        'The package must declare a name, address, telephone number, or email for consumer complaints.',
        'LMPC Rule 6(1)(a)(ii)', 'MANDATORY_DECLARATION', 'MINOR', true),
    (gen_random_uuid(), 'LM-COO-001', 'Country of origin declaration',
        'For imported packages, the country of origin/manufacture/assembly must be declared.',
        'LMPC Rule 6(1)(a)(iii)', 'MANDATORY_DECLARATION', 'MAJOR', true),
    (gen_random_uuid(), 'LM-USP-001', 'Unit sale price declaration',
        'Where applicable, the package must declare the unit sale price (price per standard unit of quantity).',
        'LMPC Rule 6(1)(e)(ii)', 'MANDATORY_DECLARATION', 'MINOR', true),
    (gen_random_uuid(), 'LM-FONTSIZE-001', 'Minimum font size for declarations',
        'Mandatory declarations must meet the minimum font size prescribed for the package''s net quantity range.',
        'LMPC Second Schedule', 'READABILITY', 'MAJOR', true),
    (gen_random_uuid(), 'LM-GENNAME-001', 'Common or generic name of commodity',
        'The package must declare the common or generic name of the commodity contained within.',
        'LMPC Rule 6(1)(b)', 'MANDATORY_DECLARATION', 'MAJOR', true)
on conflict (rule_code) do nothing;
