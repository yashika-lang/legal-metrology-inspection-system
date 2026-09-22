import type { Severity } from "@/types/enums";
import type { VisionBoundingBox } from "./vision.types";

/** Mirrors `com.legalmetrology.evidence.dto.EvidenceResponse` exactly. */
export interface EvidenceResponse {
  id: string;
  sha256Hash: string;
  inspectionId: string;
  imageId: string | null;
  violationId: string;
  originalImageUrl: string | null;
  annotatedImageUrl: string | null;
  boundingBox: VisionBoundingBox | null;
  ocrText: string | null;
  normalizedValue: string | null;
  visionConfidence: number | null;
  ocrConfidence: number | null;
  fusedConfidence: number | null;
  expectedValue: string | null;
  actualValue: string | null;
  reason: string | null;
  suggestedFix: string | null;
  legalRuleReference: string | null;
  severity: Severity;
  immutable: boolean;
  createdAt: string;
}
