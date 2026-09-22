/** Mirrors `com.legalmetrology.analytics.model.KpiSnapshot`. */
export interface KpiSnapshot {
  totalInspections: number;
  completedInspections: number;
  totalViolations: number;
  criticalViolations: number;
  /** Null (omitted from JSON) when there are zero completed inspections to average — a real, observed backend behavior, not a hypothetical. */
  averageComplianceScore: number | null;
  /** Same nullability as `averageComplianceScore`, same reason. */
  complianceRatePercent: number | null;
  activeInspectors: number;
  activeRules: number;
}

/** Mirrors `com.legalmetrology.analytics.model.MonthlyMetric`. */
export interface MonthlyMetric {
  month: string;
  count: number;
  averageValue: number | null;
}

/** Mirrors `com.legalmetrology.analytics.model.DimensionMetric`. */
export interface DimensionMetric {
  entityId: string | null;
  label: string;
  count: number;
  averageScore: number | null;
}

/** Mirrors `com.legalmetrology.analytics.model.RegionSeverityCount`. */
export interface RegionSeverityCount {
  region: string;
  severity: string;
  count: number;
}

/** Mirrors `com.legalmetrology.analytics.model.FieldCategoryCount`. */
export interface FieldCategoryCount {
  field: string;
  category: string;
  count: number;
}

/** Mirrors `com.legalmetrology.analytics.explainability.DataPeriod`. */
export interface DataPeriod {
  from: string | null;
  to: string | null;
  sampleSize: number;
  description: string;
}

/** Mirrors `com.legalmetrology.analytics.explainability.Explainability`. */
export interface Explainability {
  confidence: number;
  methodology: string;
  inputDataPeriod: DataPeriod;
  reasoning: string[];
  supportingMetrics: Record<string, unknown>;
  assumptions: string[];
  generatedAt: string;
}

export type RiskBand = "LOW" | "MEDIUM" | "HIGH";

/** Mirrors `com.legalmetrology.analytics.risk.EntityRiskScore`. */
export interface EntityRiskScore {
  entityType: string;
  entityId: string | null;
  label: string;
  riskScore: number;
  riskBand: RiskBand;
  explainability: Explainability;
}

/** Mirrors `com.legalmetrology.analytics.dto.ManufacturerDrillDownResponse`. */
export interface ManufacturerDrillDownResponse {
  manufacturerId: string;
  riskScore: EntityRiskScore;
  monthlyViolationTrend: MonthlyMetric[];
}

export type InsightType = "TREND" | "REPEAT_OFFENDER" | "CATEGORY_PATTERN" | "RULE_VERSION_IMPACT";

/** Mirrors `com.legalmetrology.analytics.insight.Insight`. */
export interface Insight {
  id: string;
  type: InsightType;
  title: string;
  message: string;
  explainability: Explainability;
}

/** Mirrors `com.legalmetrology.analytics.prediction.ConfidenceInterval`. */
export interface ConfidenceInterval {
  lowerBound: number;
  upperBound: number;
  confidenceLevel: number;
}

/** Mirrors `com.legalmetrology.analytics.prediction.Forecast`. */
export interface Forecast {
  metric: string;
  targetPeriod: string;
  predictedValue: number;
  confidenceInterval: ConfidenceInterval;
  historicalDataUsed: MonthlyMetric[];
  /** Omitted from JSON (not just null) when there's under 2 months of history — observed live, not assumed. */
  regressionEquation?: string;
  rSquared?: number | null;
  explainability: Explainability;
}

/** Mirrors `com.legalmetrology.analytics.prediction.Anomaly`. */
export interface Anomaly {
  dimension: string;
  dimensionValue: string;
  expectedValue: number;
  actualValue: number;
  zScore: number;
  threshold: number;
  reason: string;
  explainability: Explainability;
}
