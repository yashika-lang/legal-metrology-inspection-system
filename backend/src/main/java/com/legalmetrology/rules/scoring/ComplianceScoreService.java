package com.legalmetrology.rules.scoring;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Step 7: computes the 0-100 compliance score for an inspection and
 * persists it onto {@code Inspection.complianceScore} (also deriving a
 * coarse {@code fraudRisk} band from the same result). Every weight in the
 * formula is read from the database via {@link ScoringWeights} — nothing
 * here is a hardcoded constant.
 */
public interface ComplianceScoreService {

    BigDecimal computeAndPersist(UUID inspectionId);

    ScoringWeights getCurrentWeights();
}
