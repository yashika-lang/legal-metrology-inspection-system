-- Barcode-first workflow: when a scanned barcode matches a known product,
-- its master-data declarations should feed the Rule Engine directly
-- instead of requiring OCR/Vision AI to re-derive facts that are already
-- known with certainty. This adds:
--   1. A few extra informational columns on `products` that aren't part of
--      the OCR/Vision/Fusion declaration model (no rule references them).
--   2. `product_declarations` — one row per (product, declaration type),
--      the per-product equivalent of what `label_detections` would have
--      produced from a real photo, but sourced from verified master data.
--   3. A `from_product_database` flag on `fused_declarations` so the UI
--      can show a real "Product Database" source badge instead of
--      implying every fused value came from OCR/Vision AI.

ALTER TABLE products
    ADD COLUMN website VARCHAR(255),
    ADD COLUMN ingredients TEXT,
    ADD COLUMN expiry_date DATE,
    ADD COLUMN reference_image_url TEXT;

CREATE TABLE product_declarations (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id        UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    declaration_type  VARCHAR(30) NOT NULL,
    value             TEXT NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_declarations_product_type UNIQUE (product_id, declaration_type)
);

CREATE INDEX idx_product_declarations_product ON product_declarations(product_id);

CREATE TRIGGER trg_product_declarations_updated_at
    BEFORE UPDATE ON product_declarations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE fused_declarations
    ADD COLUMN from_product_database BOOLEAN NOT NULL DEFAULT false;
