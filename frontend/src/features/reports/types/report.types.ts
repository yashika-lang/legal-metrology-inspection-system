import type { DeclarationType, FraudRisk, InspectionStatus, Severity, TraceStatus, TraceStepName } from "@/types/enums";

/** Mirrors `com.legalmetrology.report.dto.ComplianceReportResponse`. */
export interface ComplianceReportResponse {
  id: string;
  inspectionId: string;
  reportNumber: string;
  pdfUrl: string;
  docxUrl: string;
  generatedByName: string;
  generatedAt: string;
}

/** Mirrors `com.legalmetrology.report.model.ReportData` and its nested records — assembled fresh, never persisted. */
export interface ReportData {
  inspectionId: string;
  reportNumber: string | null;
  generatedAt: string;
  inspection: ReportInspectionDetails;
  product: ReportProductDetails;
  compliance: ReportComplianceSummary;
  declarations: ReportDeclarationEntry[];
  violations: ReportViolationEntry[];
  evidence: ReportEvidenceEntry[];
  decisionTrace: ReportTraceEntry[];
  timeline: ReportTimelineEntry[];
  legalReferences: string[];
  executiveSummary: string;
  recommendations: string;
}

export interface ReportInspectionDetails {
  inspectorName: string;
  status: InspectionStatus;
  region: string | null;
  locationLat: number | null;
  locationLng: number | null;
  startedAt: string;
  completedAt: string | null;
}

export interface ReportProductDetails {
  productName: string | null;
  categoryName: string | null;
  manufacturerName: string | null;
  manufacturerAddress: string | null;
  barcode: string | null;
}

export interface ReportComplianceSummary {
  complianceScore: number | null;
  fraudRisk: FraudRisk | null;
  violationCount: number;
  criticalCount: number;
}

export interface ReportDeclarationEntry {
  type: DeclarationType;
  present: boolean;
  value: string | null;
  confidence: number | null;
  labelSection: string | null;
}

export interface ReportViolationEntry {
  violationId: string;
  ruleCode: string;
  ruleTitle: string;
  severity: Severity;
  field: DeclarationType | null;
  description: string;
  actualValue: string | null;
  expectedValue: string | null;
  suggestedFix: string | null;
  legalReference: string | null;
}

export interface ReportEvidenceEntry {
  evidenceId: string;
  violationId: string;
  ruleCode: string;
  severity: Severity;
  reason: string | null;
  sha256Hash: string;
  originalImageUrl: string | null;
  annotatedImageUrl: string | null;
}

export interface ReportTraceEntry {
  stepName: TraceStepName;
  module: string;
  status: TraceStatus;
  confidence: number | null;
  executionTimeMs: number;
  startedAt: string;
  outputSummary: string | null;
}

export interface ReportTimelineEntry {
  fromStatus: InspectionStatus | null;
  toStatus: InspectionStatus;
  changedByName: string | null;
  note: string | null;
  occurredAt: string;
}
