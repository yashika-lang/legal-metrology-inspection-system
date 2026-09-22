package com.legalmetrology.vision.service;

import com.legalmetrology.inspection.entity.Image;
import com.legalmetrology.vision.entity.LabelDetection;

import java.util.List;
import java.util.UUID;

/**
 * Step 4 (+ Step 6): sends an image to the configured Vision AI provider,
 * persists one {@link LabelDetection} row per declaration type it reports
 * on (present or confidently absent), and runs font analysis on each
 * detection that came back with a bounding box.
 */
public interface VisionAiService {

    List<LabelDetection> analyzeAndPersist(Image image, byte[] imageBytes, String mimeType);

    /** Self-contained variant for controller-triggered (re-)analysis: downloads the image from storage itself. */
    List<LabelDetection> analyzeAndPersist(UUID imageId);

    List<LabelDetection> getDetections(UUID imageId);
}
