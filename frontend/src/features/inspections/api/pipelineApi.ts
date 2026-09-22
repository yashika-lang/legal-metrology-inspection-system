import { apiClient, unwrap } from "@/services/apiClient";
import type { ApiResponse } from "@/types/api.types";
import type { OcrRunResponse, FusedDeclarationResponse } from "../types";
import type { LabelDetectionResponse, OcrHistoryEntryResponse } from "../types/vision.types";
import type { EvaluationResponse, ViolationResponse, ValidationResultResponse } from "../types";
import type { EvidenceResponse } from "../types";
import type { DecisionTraceStepResponse } from "../types";

/**
 * The full AI pipeline for one inspection — OCR → Vision → Fusion → Rule
 * Evaluation → Evidence → Decision Trace. Grouped here (not split into
 * five feature folders) because every one of these calls exists to feed
 * the single AI Inspection Workspace screen; splitting further is
 * premature until a second screen actually needs a subset independently.
 */
export const pipelineApi = {
  runOcr: (imageId: string) => unwrap(apiClient.post<ApiResponse<OcrRunResponse>>(`/ocr/images/${imageId}/run`)),

  runVision: (imageId: string) =>
    unwrap(apiClient.post<ApiResponse<LabelDetectionResponse[]>>(`/vision/images/${imageId}/analyze`)),

  /** Every past OCR attempt against this image, most recent first — real provider text, not a guess, for the extraction debug panel. */
  getOcrHistory: (imageId: string) =>
    unwrap(apiClient.get<ApiResponse<OcrHistoryEntryResponse[]>>(`/ocr/images/${imageId}/history`)),

  /** Vision AI detections already persisted for this image, without re-running the (paid, rate-limited) analysis call. */
  getVisionDetections: (imageId: string) =>
    unwrap(apiClient.get<ApiResponse<LabelDetectionResponse[]>>(`/vision/images/${imageId}/detections`)),

  fuseDeclarations: (inspectionId: string) =>
    unwrap(apiClient.post<ApiResponse<FusedDeclarationResponse[]>>(`/ocr/inspections/${inspectionId}/fuse`)),

  getFusedDeclarations: (inspectionId: string) =>
    unwrap(apiClient.get<ApiResponse<FusedDeclarationResponse[]>>(`/ocr/inspections/${inspectionId}/fused`)),

  evaluateRules: (inspectionId: string) =>
    unwrap(apiClient.post<ApiResponse<EvaluationResponse>>(`/rules/inspections/${inspectionId}/evaluate`)),

  getViolations: (inspectionId: string) =>
    unwrap(apiClient.get<ApiResponse<ViolationResponse[]>>(`/rules/inspections/${inspectionId}/violations`)),

  /** Every rule checked, pass or fail, not just the failures — the real signal for "which mandatory declarations are satisfied," without duplicating Rule Engine logic client-side. */
  getEvaluationResults: (inspectionId: string) =>
    unwrap(apiClient.get<ApiResponse<ValidationResultResponse[]>>(`/rules/inspections/${inspectionId}/evaluation`)),

  generateEvidence: (inspectionId: string) =>
    unwrap(apiClient.post<ApiResponse<EvidenceResponse[]>>(`/inspections/${inspectionId}/evidence/generate`)),

  getEvidence: (inspectionId: string) =>
    unwrap(apiClient.get<ApiResponse<EvidenceResponse[]>>(`/inspections/${inspectionId}/evidence`)),

  getDecisionTrace: (inspectionId: string) =>
    unwrap(apiClient.get<ApiResponse<DecisionTraceStepResponse[]>>(`/inspections/${inspectionId}/decision-trace`)),
};
