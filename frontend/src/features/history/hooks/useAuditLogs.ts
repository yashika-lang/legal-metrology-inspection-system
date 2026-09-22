import { useQuery } from "@tanstack/react-query";
import { auditLogApi, type ListAuditLogsParams } from "../api/auditLogApi";
import { queryKeys } from "@/constants/queryKeys";

export function useAuditLogs(params: ListAuditLogsParams) {
  return useQuery({
    queryKey: queryKeys.auditLogs(params),
    queryFn: () => auditLogApi.list(params),
    retry: false,
  });
}
