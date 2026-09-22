import { apiClient, unwrap } from "@/services/apiClient";
import type { ApiResponse, PagedResponse } from "@/types/api.types";
import type { InspectionRequest, InspectionResponse, InspectionStatusHistoryResponse } from "../types";

export interface ListInspectionsParams {
  mine?: boolean;
  /** Only inspections the AI Pipeline has actually evaluated (compliance score present) — excludes abandoned drafts. */
  evaluatedOnly?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export const inspectionsApi = {
  create: (payload: InspectionRequest) =>
    unwrap(apiClient.post<ApiResponse<InspectionResponse>>("/inspections", payload)),

  list: (params: ListInspectionsParams = {}) =>
    unwrap(apiClient.get<ApiResponse<PagedResponse<InspectionResponse>>>("/inspections", { params })),

  getById: (id: string) =>
    unwrap(apiClient.get<ApiResponse<InspectionResponse>>(`/inspections/${id}`)),

  updateStatus: (id: string, status: string, note?: string) =>
    unwrap(apiClient.patch<ApiResponse<InspectionResponse>>(`/inspections/${id}/status`, { status, note })),

  statusHistory: (id: string) =>
    unwrap(apiClient.get<ApiResponse<InspectionStatusHistoryResponse[]>>(`/inspections/${id}/status-history`)),
};
