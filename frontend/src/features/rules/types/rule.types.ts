import type { DeclarationType, Severity } from "@/types/enums";

export type ValidationType =
  | "FIELD_EXISTS"
  | "FIELD_NOT_EMPTY"
  | "REGEX"
  | "MIN_LENGTH"
  | "MAX_LENGTH"
  | "ENUM"
  | "DATE"
  | "NUMERIC"
  | "FONT_SIZE"
  | "READABILITY"
  | "PLACEMENT"
  | "CUSTOM";

/** Mirrors `com.legalmetrology.rules.dto.RuleResponse`. */
export interface RuleResponse {
  id: string;
  ruleCode: string;
  title: string;
  description: string | null;
  legalReference: string | null;
  category: string | null;
  severity: Severity;
  validationType: ValidationType;
  validationExpression: string;
  mandatory: boolean;
  suggestion: string | null;
  penaltyReference: string | null;
  version: number;
  effectiveDate: string;
  active: boolean;
}

/** Mirrors `com.legalmetrology.rules.dto.RuleRequest`. */
export interface RuleRequest {
  ruleCode: string;
  title: string;
  description?: string;
  legalReference?: string;
  category?: string;
  severity: Severity;
  validationType: ValidationType;
  validationExpression: string;
  mandatory: boolean;
  suggestion?: string;
  penaltyReference?: string;
}

export interface EvaluationResponse {
  inspectionId: string;
  complianceScore: number;
  fraudRisk: "LOW" | "MEDIUM" | "HIGH";
  violations: unknown[];
  results: ValidationResultResponse[];
}

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
  explanation: string;
}
