-- The Del Monte Ketchup demo product was seeded (V15) with a barcode that
-- turned out not to match the real product's actual EAN-13 — corrected to
-- the verified real value here rather than editing V15, which has already
-- been applied to this environment (editing an applied migration breaks
-- Flyway's checksum validation).
update products set barcode = '8901246003492' where barcode = '8901726003492';
