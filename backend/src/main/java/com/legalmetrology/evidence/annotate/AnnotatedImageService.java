package com.legalmetrology.evidence.annotate;

import com.legalmetrology.vision.provider.VisionBoundingBox;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Part 2 — draws violation evidence directly onto an image: bounding
 * boxes colored by severity, a label + confidence badge per box, and a
 * legend. Pure image transforms with no persistence or domain knowledge,
 * so {@code EvidenceService} (and, later, the report/dashboard renderers)
 * can all share one implementation instead of each drawing boxes their
 * own way.
 */
public interface AnnotatedImageService {

    /**
     * Annotated copy of {@code source}, capped to a 2000px longest edge —
     * still comfortably higher resolution than needed to read a bounding
     * box's label/confidence badge, and small enough to upload reliably.
     * Smaller sources are never upscaled.
     */
    byte[] annotate(BufferedImage source, List<AnnotationBox> boxes);

    /** Original and annotated images placed side by side (with a divider) for the comparison view. */
    byte[] sideBySide(byte[] originalPng, byte[] annotatedPng);

    /** Crops {@code source} to {@code box} (image-relative coordinates, clamped to the image bounds) and returns it as a standalone PNG. */
    byte[] crop(BufferedImage source, VisionBoundingBox box);
}
