package com.legalmetrology.vision.quality;

import com.legalmetrology.inspection.entity.Image;

/**
 * Facade over {@link ImageQualityAnalyzer} that also persists the result
 * onto the {@code images} row (quality_score, quality_warnings,
 * recommended_action) — the one place callers (image upload, the AI
 * pipeline orchestrator) need to know about.
 */
public interface ImageQualityService {

    QualityAnalysisResult analyzeAndPersist(Image image, byte[] imageBytes);
}
