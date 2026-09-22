import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { reportsApi } from "../api/reportsApi";
import { queryKeys } from "@/constants/queryKeys";

export function useReportsByInspection(inspectionId: string | null) {
  return useQuery({
    queryKey: queryKeys.reportsByInspection(inspectionId ?? ""),
    queryFn: () => reportsApi.listByInspection(inspectionId!),
    enabled: !!inspectionId,
  });
}

export function useReportJson(inspectionId: string | null) {
  return useQuery({
    queryKey: queryKeys.reportJson(inspectionId ?? ""),
    queryFn: () => reportsApi.getJson(inspectionId!),
    enabled: !!inspectionId,
  });
}

export function useGenerateReport(inspectionId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => reportsApi.generate(inspectionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.reportsByInspection(inspectionId) });
    },
  });
}
