package com.legalmetrology.vision.font.impl;

import com.legalmetrology.common.enums.FontIssue;
import com.legalmetrology.vision.BoundingBoxJson;
import com.legalmetrology.vision.ImageMath;
import com.legalmetrology.vision.entity.LabelDetection;
import com.legalmetrology.vision.font.FontAnalysisResult;
import com.legalmetrology.vision.font.FontAnalysisService;
import com.legalmetrology.vision.provider.VisionBoundingBox;
import com.legalmetrology.vision.repository.LabelDetectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Default {@link FontAnalysisService}. Without a physical reference scale
 * (e.g. a barcode of known real-world width visible in the same frame) an
 * image alone cannot yield an absolute millimeter font height, so
 * {@code fontSizeEstimate} is the detected text's bounding-box height as a
 * percentage of the image height — a relative, comparable-across-photos-of-
 * similar-framing proxy, not a certified physical measurement. This is
 * documented here and on the field itself so nobody mistakes it for one.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FontAnalysisServiceImpl implements FontAnalysisService {

    private static final double READABILITY_UNREADABLE_THRESHOLD = 40.0;
    private static final double FONT_SIZE_TOO_SMALL_THRESHOLD_PERCENT = 1.2;
    private static final double CONTRAST_NORMALIZATION_CAP = 90.0;
    private static final double EDGE_DENSITY_NORMALIZATION_CAP = 60.0;
    private static final int MIN_REGION_DIMENSION_PX = 3;

    private final LabelDetectionRepository labelDetectionRepository;

    @Override
    @Transactional
    public FontAnalysisResult analyzeAndPersist(LabelDetection detection, BufferedImage sourceImage) {
        VisionBoundingBox box = BoundingBoxJson.fromJson(detection.getBoundingBox());

        FontAnalysisResult result = (box == null || !detection.isPresent())
                ? hiddenResult()
                : analyzeRegion(box, sourceImage);

        detection.setFontSizeEstimate(BigDecimal.valueOf(result.fontSizeEstimate()).setScale(2, RoundingMode.HALF_UP));
        detection.setReadabilityScore(BigDecimal.valueOf(result.readabilityScore()).setScale(2, RoundingMode.HALF_UP));
        detection.setContrastScore(BigDecimal.valueOf(result.contrastScore()).setScale(2, RoundingMode.HALF_UP));
        detection.setFontIssue(result.issue());
        labelDetectionRepository.save(detection);

        return result;
    }

    private FontAnalysisResult analyzeRegion(VisionBoundingBox box, BufferedImage image) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        int x = clamp((int) (box.x() * imageWidth), 0, imageWidth - 1);
        int y = clamp((int) (box.y() * imageHeight), 0, imageHeight - 1);
        int w = clamp((int) (box.w() * imageWidth), MIN_REGION_DIMENSION_PX, imageWidth - x);
        int h = clamp((int) (box.h() * imageHeight), MIN_REGION_DIMENSION_PX, imageHeight - y);

        if (w < MIN_REGION_DIMENSION_PX || h < MIN_REGION_DIMENSION_PX) {
            return hiddenResult();
        }

        BufferedImage region = image.getSubimage(x, y, w, h);
        int[] gray = ImageMath.toGrayscale(region);
        double[] grayAsDouble = ImageMath.toDoubleArray(gray);

        double contrast = ImageMath.stdDev(grayAsDouble);
        double contrastScore = normalize(contrast, CONTRAST_NORMALIZATION_CAP);

        double[] edgeMagnitude = ImageMath.sobelMagnitude(gray, w, h);
        double edgeDensity = ImageMath.mean(edgeMagnitude);
        double edgeDensityScore = normalize(edgeDensity, EDGE_DENSITY_NORMALIZATION_CAP);

        double readabilityScore = contrastScore * 0.6 + edgeDensityScore * 0.4;
        double fontSizeEstimatePercent = (h / (double) imageHeight) * 100.0;

        FontIssue issue;
        if (readabilityScore < READABILITY_UNREADABLE_THRESHOLD) {
            issue = FontIssue.UNREADABLE;
        } else if (fontSizeEstimatePercent < FONT_SIZE_TOO_SMALL_THRESHOLD_PERCENT) {
            issue = FontIssue.TOO_SMALL;
        } else {
            issue = FontIssue.NONE;
        }

        return new FontAnalysisResult(fontSizeEstimatePercent, readabilityScore, contrastScore, issue);
    }

    private FontAnalysisResult hiddenResult() {
        return new FontAnalysisResult(0, 0, 0, FontIssue.HIDDEN);
    }

    private double normalize(double value, double cap) {
        return Math.max(0, Math.min(100, (value / cap) * 100.0));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
