-- ============================================================================
-- Demo Product Database: real, verified packaging data for four real retail
-- products, keyed by their real barcodes. When an inspector scans one of
-- these barcodes, the barcode-first workflow fetches this master data
-- instead of re-deriving it from OCR/Vision AI on every inspection — the
-- Rule Engine still evaluates it exactly as it would evaluate any other
-- fused declaration; nothing about rule evaluation is special-cased for
-- these products. Safe to re-run (guarded with on conflict do nothing).
-- To add another demo product later: insert into manufacturers/
-- product_categories if needed, then products, then product_declarations
-- keyed by that product's id — no code changes required.
-- ============================================================================

insert into manufacturers (id, name, address, region) values
    (gen_random_uuid(), 'Hindustan Unilever Limited (HUL)', 'L.B.C.P Unit II, Haridwar 249403, Uttarakhand', 'North'),
    (gen_random_uuid(), 'Ivy Laboratories Pvt Ltd', '182/3, Level 2, Industrial & Business Park, Phase 1, Chandigarh 160002', 'North'),
    (gen_random_uuid(), 'Del Monte Foods Pvt Ltd', 'Ground Floor, 31, Sarojini Devi Road, Secunderabad, 500003, Telangana', 'South')
on conflict do nothing;

insert into product_categories (id, name) values
    (gen_random_uuid(), 'Skin Care & Cosmetics'),
    (gen_random_uuid(), 'Body Spray'),
    (gen_random_uuid(), 'Packaged Food & Condiments')
on conflict do nothing;

-- Product 1: Vaseline Healthy Bright Gluta-Hya Serum-In-Lotion
insert into products (id, name, category_id, manufacturer_id, barcode, default_unit, website)
select gen_random_uuid(), 'Vaseline Healthy Bright Gluta-Hya Serum-In-Lotion Dewy Radiance',
       (select id from product_categories where name = 'Skin Care & Cosmetics'),
       (select id from manufacturers where name = 'Hindustan Unilever Limited (HUL)'),
       '8901030957048', 'ml', 'www.hul.co.in'
where not exists (select 1 from products where barcode = '8901030957048');

insert into product_declarations (product_id, declaration_type, value)
select p.id, d.declaration_type, d.value
from products p
cross join (values
    ('PRODUCT_NAME', 'Vaseline Healthy Bright Gluta-Hya Serum-In-Lotion Dewy Radiance'),
    ('MANUFACTURER', 'Hindustan Unilever Limited (HUL)'),
    ('ADDRESS', 'L.B.C.P Unit II, Haridwar 249403, Uttarakhand'),
    ('NET_QUANTITY', '200 ml'),
    ('MRP', '₹415'),
    ('CUSTOMER_CARE', '1800-10-22-221'),
    ('EMAIL', 'lever.care@unilever.com'),
    ('COUNTRY_OF_ORIGIN', 'India'),
    ('BATCH_NUMBER', 'B094'),
    ('MFG_MONTH', '2026-03'),
    ('MFG_YEAR', '2026'),
    ('LICENSE_NUMBER', 'M 16/C/UA/2010')
) as d(declaration_type, value)
where p.barcode = '8901030957048'
on conflict (product_id, declaration_type) do nothing;

-- Product 2: Re'equil Sunscreen
insert into products (id, name, category_id, manufacturer_id, barcode, default_unit, website)
select gen_random_uuid(), 'Re''equil Sunscreen',
       (select id from product_categories where name = 'Skin Care & Cosmetics'),
       (select id from manufacturers where name = 'Ivy Laboratories Pvt Ltd'),
       '8906063410867', 'g', 'www.reequil.com'
where not exists (select 1 from products where barcode = '8906063410867');

insert into product_declarations (product_id, declaration_type, value)
select p.id, d.declaration_type, d.value
from products p
cross join (values
    ('PRODUCT_NAME', 'Re''equil Sunscreen'),
    ('MANUFACTURER', 'Ivy Laboratories Pvt Ltd'),
    ('PACKER', 'Re''equil India Pvt Ltd'),
    ('ADDRESS', '182/3, Level 2, Industrial & Business Park, Phase 1, Chandigarh 160002'),
    ('NET_QUANTITY', '50 g'),
    ('CUSTOMER_CARE', '+91 7340 758 758'),
    ('EMAIL', 'care@reequil.com'),
    ('COUNTRY_OF_ORIGIN', 'India')
) as d(declaration_type, value)
where p.barcode = '8906063410867'
on conflict (product_id, declaration_type) do nothing;

-- Product 3: Body Spray (pink bottle) — deliberately incomplete real-world
-- example: only what's actually printed/known is stored, so the Rule
-- Engine correctly flags whatever is genuinely missing on this label,
-- exactly as it would for a real unscanned product.
insert into products (id, name, category_id, manufacturer_id, barcode, default_unit)
select gen_random_uuid(), 'Body Spray',
       (select id from product_categories where name = 'Body Spray'),
       null,
       '8909185805603', null
where not exists (select 1 from products where barcode = '8909185805603');

insert into product_declarations (product_id, declaration_type, value)
select p.id, d.declaration_type, d.value
from products p
cross join (values
    ('PRODUCT_NAME', 'Body Spray'),
    ('COUNTRY_OF_ORIGIN', 'India')
) as d(declaration_type, value)
where p.barcode = '8909185805603'
on conflict (product_id, declaration_type) do nothing;

-- Product 4: Del Monte Tomato Ketchup Classic Blend
insert into products (id, name, category_id, manufacturer_id, barcode, default_unit)
select gen_random_uuid(), 'Del Monte Tomato Ketchup Classic Blend',
       (select id from product_categories where name = 'Packaged Food & Condiments'),
       (select id from manufacturers where name = 'Del Monte Foods Pvt Ltd'),
       '8901726003492', 'g'
where not exists (select 1 from products where barcode = '8901726003492');

insert into product_declarations (product_id, declaration_type, value)
select p.id, d.declaration_type, d.value
from products p
cross join (values
    ('PRODUCT_NAME', 'Del Monte Tomato Ketchup Classic Blend'),
    ('MANUFACTURER', 'Del Monte Foods Pvt Ltd'),
    ('ADDRESS', 'Ground Floor, 31, Sarojini Devi Road, Secunderabad, 500003, Telangana'),
    ('NET_QUANTITY', '450 g'),
    ('CUSTOMER_CARE', '+91-124-4109436'),
    ('EMAIL', 'feedback@delmontefoodsindia.in'),
    ('LICENSE_NUMBER', '10012064000180'),
    ('COUNTRY_OF_ORIGIN', 'India')
) as d(declaration_type, value)
where p.barcode = '8901726003492'
on conflict (product_id, declaration_type) do nothing;
