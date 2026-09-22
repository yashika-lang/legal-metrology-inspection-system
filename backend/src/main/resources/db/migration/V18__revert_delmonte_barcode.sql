-- Reverting V17: the user confirmed the original barcode (8901726003492,
-- seeded in V15) was correct after all, not the value given afterward.
update products set barcode = '8901726003492' where barcode = '8901246003492';
