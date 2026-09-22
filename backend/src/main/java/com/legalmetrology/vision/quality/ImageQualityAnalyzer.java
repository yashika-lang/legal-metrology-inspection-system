package com.legalmetrology.vision.quality;

import java.awt.image.BufferedImage;

/**
 * Strategy interface for image-quality analysis (Step 1). The default
 * implementation ({@link com.legalmetrology.vision.quality.impl.PureJavaImageQualityAnalyzer})
 * uses dependency-free heuristics so this phase has no native-library
 * dependency; a future {@code OpenCvImageQualityAnalyzer} (see
 * docs/ARCHITECTURE.md "Future Ready") can implement this same interface
 * with sharper, OpenCV-backed measurements and be swapped in via a single
 * {@code @Primary}/qualifier change — no caller changes.
 */
public interface ImageQualityAnalyzer {

    QualityAnalysisResult analyze(BufferedImage image);
}
