package com.legalmetrology.vision.quality;

import com.legalmetrology.common.enums.ImageRecommendedAction;

import java.util.List;
import java.util.Set;

/**
 * Result of one image-quality pass: an overall 0–100 score, the specific
 * defects found, the raw per-metric measurements (useful for debugging and
 * for tuning thresholds), and what the officer should do next.
 */
public record QualityAnalysisResult(
        int score,
        Set<QualityWarning> warnings,
        ImageRecommendedAction recommendedAction,
        Metrics metrics
) {
    public boolean isRetakeRecommended() {
        return recommendedAction == ImageRecommendedAction.RETAKE;
    }

    /** Raw measurements behind the score/warnings, on their native (documented) scales. */
    public record Metrics(
            double blurVariance,
            double noiseEstimate,
            double rotationDegrees,
            double brightness,
            double contrast,
            double glareFraction,
            double borderContentDensity,
            double perspectiveSkew,
            int widthPx,
            int heightPx
    ) {
    }

    public static QualityAnalysisResult of(int score, Set<QualityWarning> warnings, Metrics metrics) {
        ImageRecommendedAction action;
        if (score < 45 || warnings.contains(QualityWarning.BLUR) || warnings.contains(QualityWarning.LOW_RESOLUTION)) {
            action = ImageRecommendedAction.RETAKE;
        } else if (score < 70 || !warnings.isEmpty()) {
            action = ImageRecommendedAction.ENHANCE;
        } else {
            action = ImageRecommendedAction.NONE;
        }
        return new QualityAnalysisResult(score, warnings, action, metrics);
    }

    public List<String> warningNames() {
        return warnings.stream().map(Enum::name).toList();
    }
}
