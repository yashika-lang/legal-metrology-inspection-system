/** Every enum here mirrors a `com.legalmetrology.common.enums.*` Java enum name-for-name — see backend/src/main/java/com/legalmetrology/common/enums/. */

export type InspectionStatus = "DRAFT" | "IN_PROGRESS" | "PENDING_REVIEW" | "COMPLETED" | "CLOSED";

export type FraudRisk = "LOW" | "MEDIUM" | "HIGH";

export type Severity = "CRITICAL" | "MAJOR" | "MINOR";

export type ViolationStatus = "OPEN" | "ACKNOWLEDGED" | "RESOLVED";

export type DeclarationType =
  | "PRODUCT_NAME"
  | "GENERIC_NAME"
  | "MANUFACTURER"
  | "PACKER"
  | "IMPORTER"
  | "ADDRESS"
  | "MRP"
  | "NET_QUANTITY"
  | "MFG_MONTH"
  | "MFG_YEAR"
  | "CUSTOMER_CARE"
  | "EMAIL"
  | "LICENSE_NUMBER"
  | "BATCH_NUMBER"
  | "COUNTRY_OF_ORIGIN";

export type ImageType = "FRONT" | "BACK" | "LEFT" | "RIGHT" | "TOP" | "BOTTOM" | "SIDE" | "OTHER";

/** The 6 real capture angles the Guided 360° Capture flow asks for — `SIDE`/`OTHER` stay valid backend values but aren't part of the guided flow. */
export const GUIDED_CAPTURE_ANGLES: readonly ImageType[] = ["FRONT", "BACK", "LEFT", "RIGHT", "TOP", "BOTTOM"];

export const IMAGE_TYPE_LABEL: Record<ImageType, string> = {
  FRONT: "Front",
  BACK: "Back",
  LEFT: "Left",
  RIGHT: "Right",
  TOP: "Top",
  BOTTOM: "Bottom",
  SIDE: "Side",
  OTHER: "Other",
};

export type ImageRecommendedAction = "NONE" | "RETAKE" | "ENHANCE";

/** Mirrors `com.legalmetrology.vision.quality.model.QualityWarning` — every value the Image Quality Analyzer can emit. */
export type QualityWarning =
  | "BLUR"
  | "NOISE"
  | "ROTATED"
  | "LOW_BRIGHTNESS"
  | "HIGH_BRIGHTNESS"
  | "LOW_CONTRAST"
  | "GLARE"
  | "CROPPED"
  | "LOW_RESOLUTION"
  | "PERSPECTIVE_DISTORTION";

export const QUALITY_WARNING_LABEL: Record<string, string> = {
  BLUR: "Image is blurry",
  NOISE: "High image noise",
  ROTATED: "Photo is rotated/skewed",
  LOW_BRIGHTNESS: "Too dark",
  HIGH_BRIGHTNESS: "Too bright / overexposed",
  LOW_CONTRAST: "Low contrast",
  GLARE: "Glare / reflection detected on label",
  CROPPED: "Label appears cropped",
  LOW_RESOLUTION: "Resolution too low",
  PERSPECTIVE_DISTORTION: "Camera angle distorts the label — shoot straight-on",
};

export type DetectionSource = "OCR_TEXT" | "VISION_AI";

export type LabelSection = "FRONT" | "BACK" | "SIDE" | "UNKNOWN";

export type FontIssue = "NONE" | "TOO_SMALL" | "UNREADABLE" | "HIDDEN";

export type TraceStepName =
  | "IMAGE_UPLOADED"
  | "IMAGE_QUALITY"
  | "OCR"
  | "OCR_CORRECTION"
  | "VISION_DETECTION"
  | "DECLARATION_FUSION"
  | "RULE_EVALUATION"
  | "VIOLATIONS"
  | "COMPLIANCE_SCORE"
  | "RISK_SCORE"
  | "EVIDENCE_GENERATED"
  | "REPORT_GENERATED";

export type TraceStatus = "SUCCESS" | "FAILED" | "SKIPPED";

/** Human-readable label for a pipeline step — used by the Decision Timeline. */
export const TRACE_STEP_LABEL: Record<TraceStepName, string> = {
  IMAGE_UPLOADED: "Image Uploaded",
  IMAGE_QUALITY: "Image Quality",
  OCR: "OCR",
  OCR_CORRECTION: "OCR Correction",
  VISION_DETECTION: "Vision Detection",
  DECLARATION_FUSION: "Declaration Fusion",
  RULE_EVALUATION: "Rule Evaluation",
  VIOLATIONS: "Violations",
  COMPLIANCE_SCORE: "Compliance Score",
  RISK_SCORE: "Risk Score",
  EVIDENCE_GENERATED: "Evidence",
  REPORT_GENERATED: "Report",
};

/** The canonical pipeline order for the Decision Timeline — matches the AI pipeline documented in docs/ARCHITECTURE.md. */
export const PIPELINE_ORDER: TraceStepName[] = [
  "IMAGE_UPLOADED",
  "IMAGE_QUALITY",
  "OCR",
  "OCR_CORRECTION",
  "VISION_DETECTION",
  "DECLARATION_FUSION",
  "RULE_EVALUATION",
  "VIOLATIONS",
  "COMPLIANCE_SCORE",
  "RISK_SCORE",
  "EVIDENCE_GENERATED",
  "REPORT_GENERATED",
];

export const DECLARATION_TYPE_LABEL: Record<DeclarationType, string> = {
  PRODUCT_NAME: "Product Name",
  GENERIC_NAME: "Generic Name",
  MANUFACTURER: "Manufacturer",
  PACKER: "Packer",
  IMPORTER: "Importer",
  ADDRESS: "Address",
  MRP: "MRP",
  NET_QUANTITY: "Net Quantity",
  MFG_MONTH: "Mfg. Month",
  MFG_YEAR: "Mfg. Year",
  CUSTOMER_CARE: "Consumer Care",
  EMAIL: "Email",
  LICENSE_NUMBER: "License Number",
  BATCH_NUMBER: "Batch Number",
  COUNTRY_OF_ORIGIN: "Country of Origin",
};
