import { apiClient, unwrap } from "@/services/apiClient";
import type { ApiResponse, PagedResponse } from "@/types/api.types";
import type { RuleRequest, RuleResponse } from "../types/rule.types";

export interface ListRulesParams {
  activeOnly?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export const rulesApi = {
  list: (params: ListRulesParams = {}) =>
    unwrap(apiClient.get<ApiResponse<PagedResponse<RuleResponse>>>("/rules", { params })),

  getById: (id: string) => unwrap(apiClient.get<ApiResponse<RuleResponse>>(`/rules/${id}`)),

  history: (ruleCode: string) =>
    unwrap(apiClient.get<ApiResponse<RuleResponse[]>>(`/rules/by-code/${ruleCode}/history`)),

  create: (payload: RuleRequest) => unwrap(apiClient.post<ApiResponse<RuleResponse>>("/rules", payload)),

  update: (id: string, payload: RuleRequest) =>
    unwrap(apiClient.put<ApiResponse<RuleResponse>>(`/rules/${id}`, payload)),

  deactivate: (id: string) => apiClient.delete(`/rules/${id}`),

  refreshCache: () => apiClient.post("/rules/refresh-cache"),
};
