import type { TraceStatus, TraceStepName } from "@/types/enums";

/** Mirrors `com.legalmetrology.trace.dto.DecisionTraceStepResponse` exactly. */
export interface DecisionTraceStepResponse {
  id: string;
  inspectionId: string;
  stepName: TraceStepName;
  module: string;
  inputSummary: string | null;
  outputSummary: string | null;
  confidence: number | null;
  executionTimeMs: number;
  startedAt: string;
  status: TraceStatus;
  reason: string | null;
  referencedRuleId: string | null;
  referencedEvidenceId: string | null;
  referencedImageId: string | null;
  createdAt: string;
}
