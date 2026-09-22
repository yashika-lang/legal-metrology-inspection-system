import type { DeclarationType, DetectionSource, FontIssue, LabelSection } from "@/types/enums";

/** Mirrors `com.legalmetrology.vision.provider.VisionBoundingBox` — image-relative [0,1], origin top-left. */
export interface VisionBoundingBox {
  x: number;
  y: number;
  w: number;
  h: number;
}

/** Mirrors `com.legalmetrology.vision.dto.LabelDetectionResponse`. */
export interface LabelDetectionResponse {
  id: string;
  imageId: string;
  declarationType: DeclarationType;
  detectedValue: string | null;
  confidence: number | null;
  source: DetectionSource;
  boundingBox: VisionBoundingBox | null;
  present: boolean;
  labelSection: LabelSection | null;
  fontSizeEstimate: number | null;
  readabilityScore: number | null;
  contrastScore: number | null;
  fontIssue: FontIssue | null;
}

/** Mirrors `com.legalmetrology.ocr.dto.OcrRunResponse`. */
export interface OcrRunResponse {
  id: string;
  imageId: string;
  provider: string;
  detectedLanguage: string;
  rawText: string;
  correctedText: string;
  ocrConfidence: number;
  correctionConfidence: number;
  correctionChangesSummary: string | null;
  fields: LabelDetectionResponse[];
  runAt: string;
}

/** Mirrors `com.legalmetrology.ocr.dto.OcrHistoryEntryResponse` — one past OCR run against a single image. */
export interface OcrHistoryEntryResponse {
  id: string;
  provider: string;
  detectedLanguage: string | null;
  rawText: string | null;
  correctedText: string | null;
  confidence: number | null;
  correctionConfidence: number | null;
  runAt: string;
}

/** Mirrors `com.legalmetrology.ocr.dto.FusedDeclarationResponse` exactly. */
export interface FusedDeclarationResponse {
  id: string;
  inspectionId: string;
  declarationType: DeclarationType;
  fusedValue: string | null;
  fusedConfidence: number | null;
  present: boolean;
  ocrValue: string | null;
  ocrConfidence: number | null;
  visionValue: string | null;
  visionConfidence: number | null;
  agreement: boolean | null;
  fromProductDatabase: boolean;
}
