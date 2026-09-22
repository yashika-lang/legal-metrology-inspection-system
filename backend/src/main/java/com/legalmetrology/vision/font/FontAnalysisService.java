package com.legalmetrology.vision.font;

import com.legalmetrology.vision.entity.LabelDetection;

import java.awt.image.BufferedImage;

/**
 * Step 6: estimates font size, readability, visibility, and contrast for a
 * single detected declaration, and persists the result onto that
 * {@link LabelDetection} row.
 */
public interface FontAnalysisService {

    FontAnalysisResult analyzeAndPersist(LabelDetection detection, BufferedImage sourceImage);
}
