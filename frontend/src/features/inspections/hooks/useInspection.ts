import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { inspectionsApi, type ListInspectionsParams } from "../api/inspectionsApi";
import { queryKeys } from "@/constants/queryKeys";
import type { InspectionRequest } from "../types";

export function useInspections(params: ListInspectionsParams) {
  return useQuery({
    queryKey: queryKeys.inspectionsList(params),
    queryFn: () => inspectionsApi.list(params),
  });
}

export function useInspection(inspectionId: string | undefined) {
  return useQuery({
    queryKey: queryKeys.inspection(inspectionId ?? ""),
    queryFn: () => inspectionsApi.getById(inspectionId!),
    enabled: !!inspectionId,
  });
}

export function useCreateInspection() {
  return useMutation({
    mutationFn: (payload: InspectionRequest) => inspectionsApi.create(payload),
  });
}

export function useUpdateInspectionStatus(inspectionId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ status, note }: { status: string; note?: string }) =>
      inspectionsApi.updateStatus(inspectionId, status, note),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.inspection(inspectionId) });
    },
  });
}
