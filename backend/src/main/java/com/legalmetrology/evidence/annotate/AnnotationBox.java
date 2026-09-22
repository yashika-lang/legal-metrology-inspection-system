package com.legalmetrology.evidence.annotate;

import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.vision.provider.VisionBoundingBox;

import java.math.BigDecimal;

/** One labeled region to draw on an annotated image — the field name, its severity color, and its confidence badge. */
public record AnnotationBox(VisionBoundingBox box, String label, Severity severity, BigDecimal confidence) {
}
