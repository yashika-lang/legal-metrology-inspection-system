import type { DeclarationType, FraudRisk, Severity, ViolationStatus } from "@/types/enums";
import type { VisionBoundingBox } from "./vision.types";

/** Mirrors `com.legalmetrology.rules.dto.ViolationResponse`. */
export interface ViolationResponse {
  id: string;
  inspectionId: string;
  ruleId: string;
  ruleCode: string;
  ruleTitle: string;
  severity: Severity;
  field: DeclarationType | null;
  description: string;
  actualValue: string | null;
  expectedValue: string | null;
  confidence: number;
  boundingBox: VisionBoundingBox | null;
  suggestedFix: string | null;
  legalReference: string | null;
  status: ViolationStatus;
  createdAt: string;
}

/** Mirrors `com.legalmetrology.rules.dto.ValidationResultResponse` — every rule checked, pass or fail (not persisted). */
export interface ValidationResultResponse {
  ruleId: string;
  ruleCode: string;
  ruleTitle: string;
  severity: Severity;
  passed: boolean;
  field: DeclarationType | null;
  actualValue: string | null;
  expectedValue: string | null;
  confidence: number;
  boundingBox: VisionBoundingBox | null;
  explanation: string;
}

/** Mirrors `com.legalmetrology.rules.dto.EvaluationResponse`. */
export interface EvaluationResponse {
  inspectionId: string;
  complianceScore: number;
  fraudRisk: FraudRisk;
  violations: ViolationResponse[];
  results: ValidationResultResponse[];
}
