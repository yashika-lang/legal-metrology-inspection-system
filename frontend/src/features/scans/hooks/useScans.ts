import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { scansApi } from "../api/scansApi";
import { queryKeys } from "@/constants/queryKeys";
import type { ScanType } from "../types/scan.types";

export function useInspectionScans(inspectionId: string) {
  return useQuery({
    queryKey: queryKeys.scans(inspectionId),
    queryFn: () => scansApi.listByInspection(inspectionId),
    enabled: !!inspectionId,
  });
}

export function useRecordScan(inspectionId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ scanType, scanValue }: { scanType: ScanType; scanValue: string }) =>
      scansApi.record(inspectionId, scanType, scanValue),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.scans(inspectionId) });
    },
  });
}
