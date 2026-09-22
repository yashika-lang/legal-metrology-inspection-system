/**
 * Centralized, typed query-key factories — every TanStack Query hook uses
 * these instead of inline string arrays, so cache invalidation after a
 * mutation can't typo-drift from the key the list query actually used.
 */
export const queryKeys = {
  inspection: (id: string) => ["inspection", id] as const,
  inspectionImages: (inspectionId: string) => ["inspection", inspectionId, "images"] as const,
  imageOcrHistory: (imageId: string) => ["image", imageId, "ocr-history"] as const,
  imageVisionDetections: (imageId: string) => ["image", imageId, "vision-detections"] as const,
  fusedDeclarations: (inspectionId: string) => ["inspection", inspectionId, "fused"] as const,
  violations: (inspectionId: string) => ["inspection", inspectionId, "violations"] as const,
  evaluationResults: (inspectionId: string) => ["inspection", inspectionId, "evaluation-results"] as const,
  evidence: (inspectionId: string) => ["inspection", inspectionId, "evidence"] as const,
  decisionTrace: (inspectionId: string) => ["inspection", inspectionId, "decision-trace"] as const,
  scans: (inspectionId: string) => ["inspection", inspectionId, "scans"] as const,
  copilotHistory: (inspectionId: string) => ["inspection", inspectionId, "copilot-history"] as const,
  currentUser: ["auth", "me"] as const,

  inspectionsList: (params: object) => ["inspections", "list", params] as const,

  kpis: ["analytics", "kpis"] as const,
  timeseries: (metric: string, months: number) => ["analytics", "timeseries", metric, months] as const,
  heatmap: (kind: string) => ["analytics", "heatmap", kind] as const,
  dimensionMetric: (kind: string) => ["analytics", "dimension", kind] as const,
  manufacturerDrilldown: (id: string, months: number) => ["analytics", "drilldown", id, months] as const,
  insights: ["analytics", "insights"] as const,
  forecast: ["analytics", "forecast"] as const,
  anomalies: ["analytics", "anomalies"] as const,
  riskIndex: (entity: string) => ["analytics", "risk", entity] as const,

  products: (params: object) => ["products", "list", params] as const,
  product: (id: string) => ["products", id] as const,
  productCategories: ["products", "categories"] as const,
  manufacturers: ["products", "manufacturers"] as const,

  reportsByInspection: (inspectionId: string) => ["reports", "inspection", inspectionId] as const,
  reportJson: (inspectionId: string) => ["reports", "inspection", inspectionId, "json"] as const,

  auditLogs: (params: object) => ["history", "audit-logs", params] as const,

  rules: (params: object) => ["rules", "list", params] as const,
  rule: (id: string) => ["rules", id] as const,
  ruleHistory: (ruleCode: string) => ["rules", "history", ruleCode] as const,

  settings: ["settings", "list"] as const,
};
