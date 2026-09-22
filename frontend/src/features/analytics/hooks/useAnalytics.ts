import { useQuery } from "@tanstack/react-query";
import { analyticsApi } from "../api/analyticsApi";
import { queryKeys } from "@/constants/queryKeys";

export function useKpis() {
  return useQuery({ queryKey: queryKeys.kpis, queryFn: analyticsApi.kpis });
}

export function useTimeseries(metric: "violations" | "inspections" | "compliance-score", months = 6) {
  return useQuery({
    queryKey: queryKeys.timeseries(metric, months),
    queryFn: () => {
      if (metric === "violations") return analyticsApi.timeseriesViolations(months);
      if (metric === "inspections") return analyticsApi.timeseriesInspections(months);
      return analyticsApi.timeseriesComplianceScore(months);
    },
  });
}

export function useRegionSeverityHeatmap() {
  return useQuery({ queryKey: queryKeys.heatmap("region-severity"), queryFn: analyticsApi.heatmapRegionSeverity });
}

export function useFieldCategoryHeatmap() {
  return useQuery({ queryKey: queryKeys.heatmap("field-category"), queryFn: analyticsApi.heatmapFieldCategory });
}

export function useSeverityDistribution() {
  return useQuery({ queryKey: queryKeys.dimensionMetric("severity-distribution"), queryFn: analyticsApi.severityDistribution });
}

export function useLeaderboard(kind: "rules" | "manufacturers" | "inspectors") {
  return useQuery({
    queryKey: queryKeys.dimensionMetric(`leaderboard-${kind}`),
    queryFn: () => {
      if (kind === "rules") return analyticsApi.leaderboardRules();
      if (kind === "manufacturers") return analyticsApi.leaderboardManufacturers();
      return analyticsApi.leaderboardInspectors();
    },
  });
}

export function useManufacturerDrilldown(manufacturerId: string | null, months = 6) {
  return useQuery({
    queryKey: queryKeys.manufacturerDrilldown(manufacturerId ?? "", months),
    queryFn: () => analyticsApi.manufacturerDrilldown(manufacturerId!, months),
    enabled: !!manufacturerId,
  });
}

export function useInsights() {
  return useQuery({ queryKey: queryKeys.insights, queryFn: analyticsApi.insights });
}

export function useForecast() {
  return useQuery({ queryKey: queryKeys.forecast, queryFn: analyticsApi.forecast });
}

export function useAnomalies() {
  return useQuery({ queryKey: queryKeys.anomalies, queryFn: analyticsApi.anomalies });
}

export function useRiskIndex(entity: "manufacturers" | "categories" | "regions" | "inspectors") {
  return useQuery({
    queryKey: queryKeys.riskIndex(entity),
    queryFn: () => {
      if (entity === "manufacturers") return analyticsApi.riskManufacturers();
      if (entity === "categories") return analyticsApi.riskCategories();
      if (entity === "regions") return analyticsApi.riskRegions();
      return analyticsApi.riskInspectors();
    },
  });
}
