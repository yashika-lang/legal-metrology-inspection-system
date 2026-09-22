-- The Vaseline demo product's real label prints "SKIN LOTION" as its
-- generic/common name (confirmed from a real photo of this exact product
-- reviewed earlier) — added separately from V15 since that migration had
-- already been applied to this environment by the time this was noticed.
insert into product_declarations (product_id, declaration_type, value)
select p.id, 'GENERIC_NAME', 'SKIN LOTION'
from products p
where p.barcode = '8901030957048'
on conflict (product_id, declaration_type) do nothing;
