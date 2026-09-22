import { apiClient, unwrap } from "@/services/apiClient";
import type { ApiResponse } from "@/types/api.types";
import type {
  KpiSnapshot,
  MonthlyMetric,
  DimensionMetric,
  RegionSeverityCount,
  FieldCategoryCount,
  EntityRiskScore,
  ManufacturerDrillDownResponse,
  Insight,
  Forecast,
  Anomaly,
} from "../types/analytics.types";

export const analyticsApi = {
  kpis: () => unwrap(apiClient.get<ApiResponse<KpiSnapshot>>("/analytics/kpis")),

  timeseriesViolations: (months = 6) =>
    unwrap(apiClient.get<ApiResponse<MonthlyMetric[]>>("/analytics/timeseries/violations", { params: { months } })),

  timeseriesInspections: (months = 6) =>
    unwrap(apiClient.get<ApiResponse<MonthlyMetric[]>>("/analytics/timeseries/inspections", { params: { months } })),

  timeseriesComplianceScore: (months = 6) =>
    unwrap(
      apiClient.get<ApiResponse<MonthlyMetric[]>>("/analytics/timeseries/compliance-score", { params: { months } }),
    ),

  heatmapRegionSeverity: () =>
    unwrap(apiClient.get<ApiResponse<RegionSeverityCount[]>>("/analytics/heatmap/region-severity")),

  heatmapFieldCategory: () =>
    unwrap(apiClient.get<ApiResponse<FieldCategoryCount[]>>("/analytics/heatmap/field-category")),

  treemapManufacturers: () =>
    unwrap(apiClient.get<ApiResponse<DimensionMetric[]>>("/analytics/treemap/manufacturers")),

  treemapCategories: () => unwrap(apiClient.get<ApiResponse<DimensionMetric[]>>("/analytics/treemap/categories")),

  leaderboardRules: (limit = 10) =>
    unwrap(apiClient.get<ApiResponse<DimensionMetric[]>>("/analytics/leaderboard/rules", { params: { limit } })),

  leaderboardManufacturers: () =>
    unwrap(apiClient.get<ApiResponse<DimensionMetric[]>>("/analytics/leaderboard/manufacturers")),

  leaderboardInspectors: () =>
    unwrap(apiClient.get<ApiResponse<DimensionMetric[]>>("/analytics/leaderboard/inspectors")),

  severityDistribution: () =>
    unwrap(apiClient.get<ApiResponse<DimensionMetric[]>>("/analytics/severity-distribution")),

  manufacturerDrilldown: (manufacturerId: string, months = 6) =>
    unwrap(
      apiClient.get<ApiResponse<ManufacturerDrillDownResponse>>(
        `/analytics/drilldown/manufacturers/${manufacturerId}`,
        { params: { months } },
      ),
    ),

  insights: () => unwrap(apiClient.get<ApiResponse<Insight[]>>("/analytics/insights")),

  forecast: () => unwrap(apiClient.get<ApiResponse<Forecast[]>>("/analytics/predictions/forecast")),

  anomalies: () => unwrap(apiClient.get<ApiResponse<Anomaly[]>>("/analytics/predictions/anomalies")),

  riskManufacturers: () => unwrap(apiClient.get<ApiResponse<EntityRiskScore[]>>("/analytics/risk/manufacturers")),

  riskCategories: () => unwrap(apiClient.get<ApiResponse<EntityRiskScore[]>>("/analytics/risk/categories")),

  riskRegions: () => unwrap(apiClient.get<ApiResponse<EntityRiskScore[]>>("/analytics/risk/regions")),

  riskInspectors: () => unwrap(apiClient.get<ApiResponse<EntityRiskScore[]>>("/analytics/risk/inspectors")),
};
