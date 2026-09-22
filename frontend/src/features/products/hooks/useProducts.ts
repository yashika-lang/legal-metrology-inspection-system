import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { productsApi, type ListProductsParams } from "../api/productsApi";
import { queryKeys } from "@/constants/queryKeys";
import type { ProductRequest } from "../types/product.types";

export function useProducts(params: ListProductsParams) {
  return useQuery({
    queryKey: queryKeys.products(params),
    queryFn: () => productsApi.list(params),
  });
}

export function useProductCategories() {
  return useQuery({ queryKey: queryKeys.productCategories, queryFn: productsApi.categories });
}

export function useCreateProductCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (name: string) => productsApi.createCategory(name),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.productCategories });
    },
  });
}

export function useManufacturers() {
  return useQuery({ queryKey: queryKeys.manufacturers, queryFn: productsApi.manufacturers });
}

export function useCreateManufacturer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (name: string) => productsApi.createManufacturer(name),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.manufacturers });
    },
  });
}

export function useCreateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ProductRequest) => productsApi.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["products", "list"] });
    },
  });
}

export function useDeleteProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => productsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["products", "list"] });
    },
  });
}
