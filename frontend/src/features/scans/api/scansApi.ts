import { apiClient, unwrap } from "@/services/apiClient";
import type { ApiResponse } from "@/types/api.types";
import type { ScanResponse, ScanType } from "../types/scan.types";

export const scansApi = {
  record: (inspectionId: string, scanType: ScanType, scanValue: string) =>
    unwrap(
      apiClient.post<ApiResponse<ScanResponse>>("/scans", { inspectionId, scanType, scanValue }),
    ),

  listByInspection: (inspectionId: string) =>
    unwrap(apiClient.get<ApiResponse<ScanResponse[]>>(`/scans/inspections/${inspectionId}`)),
};
