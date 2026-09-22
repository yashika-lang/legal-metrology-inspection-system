import { apiClient, unwrap } from "@/services/apiClient";
import type { ApiResponse, PagedResponse } from "@/types/api.types";
import type { AuditLogResponse } from "../types/auditLog.types";

export interface ListAuditLogsParams {
  entityType?: string;
  entityId?: string;
  userId?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export const auditLogApi = {
  list: (params: ListAuditLogsParams = {}) =>
    unwrap(apiClient.get<ApiResponse<PagedResponse<AuditLogResponse>>>("/history/audit-logs", { params })),
};
