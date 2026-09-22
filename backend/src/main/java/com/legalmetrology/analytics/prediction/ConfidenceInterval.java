package com.legalmetrology.analytics.prediction;

import java.math.BigDecimal;

/** A range, not a point guess — the honest way to present a forecast. {@code confidenceLevel} is 0-1 (e.g. 0.95 for a 95% interval). */
public record ConfidenceInterval(BigDecimal lowerBound, BigDecimal upperBound, double confidenceLevel) {
}
