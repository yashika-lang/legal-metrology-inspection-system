import { apiClient, unwrap } from "@/services/apiClient";
import type { ApiResponse } from "@/types/api.types";
import type { ComplianceReportResponse, ReportData } from "../types/report.types";

export const reportsApi = {
  listByInspection: (inspectionId: string) =>
    unwrap(apiClient.get<ApiResponse<ComplianceReportResponse[]>>(`/reports/inspections/${inspectionId}`)),

  generate: (inspectionId: string) =>
    unwrap(apiClient.post<ApiResponse<ComplianceReportResponse>>(`/reports/inspections/${inspectionId}/generate`)),

  getJson: (inspectionId: string) =>
    unwrap(apiClient.get<ApiResponse<ReportData>>(`/reports/inspections/${inspectionId}/json`)),
};
