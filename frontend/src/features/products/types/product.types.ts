/** Mirrors `com.legalmetrology.product.dto.ProductResponse`. */
export interface ProductResponse {
  id: string;
  name: string;
  categoryId: string | null;
  categoryName: string | null;
  manufacturerId: string | null;
  manufacturerName: string | null;
  barcode: string | null;
  defaultUnit: string | null;
  createdAt: string;
}

/** Mirrors `com.legalmetrology.product.dto.ProductRequest`. */
export interface ProductRequest {
  name: string;
  categoryId?: string | null;
  manufacturerId?: string | null;
  barcode?: string | null;
  defaultUnit?: string | null;
}

/** Mirrors `com.legalmetrology.product.dto.ProductCategoryResponse`. */
export interface ProductCategoryResponse {
  id: string;
  name: string;
  parentCategoryId: string | null;
}

/** Mirrors `com.legalmetrology.product.dto.ManufacturerResponse`. */
export interface ManufacturerResponse {
  id: string;
  name: string;
  gstin: string | null;
  address: string | null;
  region: string | null;
}
