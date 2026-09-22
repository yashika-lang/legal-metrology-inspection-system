package com.legalmetrology.common.enums;

/** The fixed pipeline stages a {@code DecisionTraceStep} can record — mirrors the documented Image → ... → Report pipeline. */
public enum TraceStepName {
    IMAGE_UPLOADED,
    IMAGE_QUALITY,
    OCR,
    OCR_CORRECTION,
    VISION_DETECTION,
    DECLARATION_FUSION,
    RULE_EVALUATION,
    VIOLATIONS,
    COMPLIANCE_SCORE,
    RISK_SCORE,
    EVIDENCE_GENERATED,
    REPORT_GENERATED
}
