package com.legalmetrology.rules.scoring;

import java.math.BigDecimal;

/**
 * Every number the compliance-score formula uses, sourced from the
 * {@code settings} table (keys prefixed {@code scoring.}) — never
 * hardcoded. See {@code rules.scoring.impl.ComplianceScoreServiceImpl} for
 * the formula these plug into and V7's seed data for the shipped defaults.
 */
public record ScoringWeights(
        BigDecimal criticalWeight,
        BigDecimal majorWeight,
        BigDecimal minorWeight,
        BigDecimal imageQualityWeight,
        BigDecimal ocrConfidenceWeight,
        BigDecimal fontReadabilityWeight,
        BigDecimal missingFieldsWeight
) {
}
