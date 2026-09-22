import { apiClient, unwrap } from "@/services/apiClient";
import type { ApiResponse, PagedResponse } from "@/types/api.types";
import type { ManufacturerResponse, ProductCategoryResponse, ProductRequest, ProductResponse } from "../types/product.types";

export interface ListProductsParams {
  query?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export const productsApi = {
  list: (params: ListProductsParams = {}) =>
    unwrap(apiClient.get<ApiResponse<PagedResponse<ProductResponse>>>("/products", { params })),

  getById: (id: string) => unwrap(apiClient.get<ApiResponse<ProductResponse>>(`/products/${id}`)),

  getByBarcode: (barcode: string) =>
    unwrap(apiClient.get<ApiResponse<ProductResponse>>(`/products/barcode/${barcode}`)),

  create: (payload: ProductRequest) => unwrap(apiClient.post<ApiResponse<ProductResponse>>("/products", payload)),

  update: (id: string, payload: ProductRequest) =>
    unwrap(apiClient.put<ApiResponse<ProductResponse>>(`/products/${id}`, payload)),

  delete: (id: string) => apiClient.delete(`/products/${id}`),

  categories: () => unwrap(apiClient.get<ApiResponse<ProductCategoryResponse[]>>("/products/categories")),

  createCategory: (name: string) =>
    unwrap(apiClient.post<ApiResponse<ProductCategoryResponse>>("/products/categories", { name })),

  manufacturers: () => unwrap(apiClient.get<ApiResponse<ManufacturerResponse[]>>("/products/manufacturers")),

  createManufacturer: (name: string) =>
    unwrap(apiClient.post<ApiResponse<ManufacturerResponse>>("/products/manufacturers", { name })),
};
