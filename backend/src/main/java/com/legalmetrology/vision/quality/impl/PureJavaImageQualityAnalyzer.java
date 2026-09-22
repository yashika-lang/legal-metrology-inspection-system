package com.legalmetrology.vision.quality.impl;

import com.legalmetrology.vision.ImageMath;
import com.legalmetrology.vision.quality.ImageQualityAnalyzer;
import com.legalmetrology.vision.quality.QualityAnalysisResult;
import com.legalmetrology.vision.quality.QualityWarning;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Default {@link ImageQualityAnalyzer}: dependency-free heuristics computed
 * directly on {@link BufferedImage} pixel data (via {@link ImageMath}) — no
 * OpenCV/native binding required, so this runs anywhere the JVM does. Each
 * check is a standard, well-understood signal-processing technique
 * (Laplacian-variance blur detection, Sobel gradient-orientation skew
 * estimation, high-frequency residual noise estimation, HSB-based glare
 * detection, border edge-density cropping detection) rather than a black
 * box — see the inline Javadoc on each {@code detect*}/{@code estimate*}
 * method for the specific technique and its known limitations.
 * <p>
 * Analysis runs on a downscaled working copy (longest side capped at
 * {@link #WORKING_MAX_DIMENSION}px) purely for speed; the resolution check
 * itself still uses the original image's dimensions.
 */
@Component
public class PureJavaImageQualityAnalyzer implements ImageQualityAnalyzer {

    private static final int WORKING_MAX_DIMENSION = 900;

    private static final double BLUR_VARIANCE_THRESHOLD = 55.0;
    private static final double NOISE_THRESHOLD = 9.0;
    private static final double ROTATION_DEGREES_THRESHOLD = 5.0;
    private static final double LOW_BRIGHTNESS_THRESHOLD = 60.0;
    private static final double HIGH_BRIGHTNESS_THRESHOLD = 205.0;
    private static final double LOW_CONTRAST_THRESHOLD = 28.0;
    private static final double GLARE_FRACTION_THRESHOLD = 0.03;
    private static final double GLARE_LUMINANCE_THRESHOLD = 245.0;
    private static final float GLARE_SATURATION_THRESHOLD = 0.15f;
    private static final double BORDER_DENSITY_RATIO_THRESHOLD = 0.65;
    private static final int MIN_ACCEPTABLE_DIMENSION_PX = 600;
    private static final double NOISE_FLAT_REGION_GRADIENT_THRESHOLD = 20.0;
    private static final double ROTATION_EDGE_GRADIENT_THRESHOLD = 40.0;
    private static final int PERSPECTIVE_HISTOGRAM_WINDOW_DEGREES = 30;
    // Deliberately conservative: a real product label's text contributes many
    // short, disconnected glyph edges at a wide range of angles (unlike the
    // package's own long, continuous boundary edges), which this histogram
    // approach cannot distinguish from genuine perspective convergence. Found
    // live: two ordinary (non-warped) real-content test uploads both crossed
    // an earlier, tighter threshold (12°) — a false positive on typical,
    // legitimately-shot label photos is worse than missing a real one, so
    // this only fires on skew large enough to be unambiguous.
    private static final double PERSPECTIVE_SKEW_DEGREES_THRESHOLD = 30.0;
    private static final int PERSPECTIVE_MIN_EDGE_SAMPLES = 800;

    @Override
    public QualityAnalysisResult analyze(BufferedImage original) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        BufferedImage working = downscale(original, WORKING_MAX_DIMENSION);
        int width = working.getWidth();
        int height = working.getHeight();
        int[] gray = ImageMath.toGrayscale(working);
        double[] grayAsDouble = ImageMath.toDoubleArray(gray);

        double[] laplacian = laplacian(gray, width, height);
        double blurVariance = ImageMath.variance(laplacian);

        double[] noiseSamples = highFrequencyResidualInFlatRegions(gray, width, height);
        double noiseEstimate = noiseSamples.length == 0 ? 0 : ImageMath.mean(noiseSamples);

        int[] orientationHistogram = buildOrientationHistogram(gray, width, height);
        double rotationDegrees = rotationDegreesFromHistogram(orientationHistogram);
        double perspectiveSkew = perspectiveSkewFromHistogram(orientationHistogram);

        double brightness = ImageMath.mean(grayAsDouble);
        double contrast = ImageMath.stdDev(grayAsDouble);

        double glareFraction = detectGlareFraction(working);
        double borderDensityRatio = detectBorderContentDensityRatio(laplacian, width, height);

        Set<QualityWarning> warnings = EnumSet.noneOf(QualityWarning.class);
        if (blurVariance < BLUR_VARIANCE_THRESHOLD) warnings.add(QualityWarning.BLUR);
        if (noiseEstimate > NOISE_THRESHOLD) warnings.add(QualityWarning.NOISE);
        if (rotationDegrees > ROTATION_DEGREES_THRESHOLD) warnings.add(QualityWarning.ROTATED);
        if (brightness < LOW_BRIGHTNESS_THRESHOLD) warnings.add(QualityWarning.LOW_BRIGHTNESS);
        if (brightness > HIGH_BRIGHTNESS_THRESHOLD) warnings.add(QualityWarning.HIGH_BRIGHTNESS);
        if (contrast < LOW_CONTRAST_THRESHOLD) warnings.add(QualityWarning.LOW_CONTRAST);
        if (glareFraction > GLARE_FRACTION_THRESHOLD) warnings.add(QualityWarning.GLARE);
        if (borderDensityRatio > BORDER_DENSITY_RATIO_THRESHOLD) warnings.add(QualityWarning.CROPPED);
        if (originalWidth < MIN_ACCEPTABLE_DIMENSION_PX || originalHeight < MIN_ACCEPTABLE_DIMENSION_PX) {
            warnings.add(QualityWarning.LOW_RESOLUTION);
        }
        if (perspectiveSkew > PERSPECTIVE_SKEW_DEGREES_THRESHOLD) warnings.add(QualityWarning.PERSPECTIVE_DISTORTION);

        int score = computeScore(warnings);

        var metrics = new QualityAnalysisResult.Metrics(
                blurVariance, noiseEstimate, rotationDegrees, brightness, contrast,
                glareFraction, borderDensityRatio, perspectiveSkew, originalWidth, originalHeight);

        return QualityAnalysisResult.of(score, warnings, metrics);
    }

    private int computeScore(Set<QualityWarning> warnings) {
        int score = 100;
        for (QualityWarning warning : warnings) {
            score -= switch (warning) {
                case BLUR -> 30;
                case LOW_RESOLUTION -> 25;
                case CROPPED -> 20;
                case LOW_BRIGHTNESS, LOW_CONTRAST, GLARE -> 15;
                case NOISE, ROTATED, HIGH_BRIGHTNESS -> 10;
                case PERSPECTIVE_DISTORTION -> 15;
            };
        }
        return Math.max(0, Math.min(100, score));
    }

    private BufferedImage downscale(BufferedImage source, int maxDimension) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longestSide = Math.max(width, height);
        if (longestSide <= maxDimension) {
            return source;
        }
        double scale = maxDimension / (double) longestSide;
        int newWidth = Math.max(1, (int) (width * scale));
        int newHeight = Math.max(1, (int) (height * scale));

        Image scaled = source.getScaledInstance(newWidth, newHeight, Image.SCALE_AREA_AVERAGING);
        BufferedImage result = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        result.getGraphics().drawImage(scaled, 0, 0, null);
        return result;
    }

    /**
     * Blur detection via Laplacian variance: a sharp image has strong,
     * varied second-derivative response at edges; a blurred image's edges
     * are smeared, so the Laplacian response is low and uniform (low
     * variance). Standard technique (Pech-Pacheco et al., 2000).
     */
    private double[] laplacian(int[] gray, int width, int height) {
        double[] response = new double[width * height];
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int center = gray[y * width + x];
                int up = gray[(y - 1) * width + x];
                int down = gray[(y + 1) * width + x];
                int left = gray[y * width + (x - 1)];
                int right = gray[y * width + (x + 1)];
                response[y * width + x] = (up + down + left + right - 4.0 * center);
            }
        }
        return response;
    }

    /**
     * Noise estimate: residual energy between the image and a 3x3
     * box-blurred version of itself, sampled only in locally flat regions
     * (low Sobel gradient magnitude) so real edges/text aren't mistaken for
     * sensor noise — noise lives in the high-frequency residual of flat
     * areas; edges live in the residual of gradient areas.
     */
    private double[] highFrequencyResidualInFlatRegions(int[] gray, int width, int height) {
        double[] sobelMagnitude = ImageMath.sobelMagnitude(gray, width, height);

        List<Double> samples = new ArrayList<>();
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int idx = y * width + x;
                if (sobelMagnitude[idx] > NOISE_FLAT_REGION_GRADIENT_THRESHOLD) {
                    continue; // skip edges/text — only measure noise in flat background
                }
                double boxBlur = (gray[idx - 1] + gray[idx + 1] + gray[idx - width] + gray[idx + width]
                        + gray[idx - width - 1] + gray[idx - width + 1] + gray[idx + width - 1] + gray[idx + width + 1]
                        + gray[idx]) / 9.0;
                samples.add(Math.abs(gray[idx] - boxBlur));
            }
        }
        return samples.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /**
     * Builds the gradient-orientation histogram shared by the rotation and
     * perspective-distortion estimates below: strong edge pixels' gradient
     * directions are binned (mod 180°, since an edge and its reverse point
     * the same "line"). A well-aligned rectangular label produces a tight,
     * dominant orientation near 0° or 90°.
     */
    private int[] buildOrientationHistogram(int[] gray, int width, int height) {
        int[] histogram = new int[180];

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                double gx = ImageMath.sobelGx(gray, width, x, y);
                double gy = ImageMath.sobelGy(gray, width, x, y);
                double magnitude = Math.sqrt(gx * gx + gy * gy);
                if (magnitude < ROTATION_EDGE_GRADIENT_THRESHOLD) {
                    continue;
                }
                double angleDegrees = Math.toDegrees(Math.atan2(gy, gx));
                int bin = ((int) Math.round(angleDegrees) % 180 + 180) % 180;
                histogram[bin]++;
            }
        }
        return histogram;
    }

    private int peakBin(int[] histogram) {
        int peakBin = 0;
        for (int i = 1; i < histogram.length; i++) {
            if (histogram[i] > histogram[peakBin]) {
                peakBin = i;
            }
        }
        return peakBin;
    }

    /**
     * Rotation/skew estimate: the dominant orientation's distance from the
     * nearer of the two "well-aligned" axes (0°/90°). This is a simplified
     * relative of the projection-profile skew-detection family used for
     * document deskewing — it estimates *label* skew, not perfect
     * text-line skew, and is intentionally conservative (large, confident
     * deviations only) to avoid false positives on naturally busy labels.
     */
    private double rotationDegreesFromHistogram(int[] histogram) {
        int peakBin = peakBin(histogram);
        double distanceFromZero = Math.min(peakBin, 180 - peakBin);
        double distanceFromNinety = Math.abs(peakBin - 90);
        return Math.min(distanceFromZero, distanceFromNinety);
    }

    /**
     * Perspective-distortion (keystoning) estimate, distinct from simple
     * rotation: a camera held at an angle to a flat rectangular label makes
     * its two "vertical" edges converge rather than stay parallel — so
     * instead of one tight cluster of edge angles, the dominant orientation
     * band spreads across a wider range (the left edge and right edge no
     * longer point the same direction). A uniform in-plane rotation shifts
     * the whole peak but keeps it tight; only the *spread* is a perspective
     * signal. Measured as the weighted circular standard deviation of edge
     * angles within {@link #PERSPECTIVE_HISTOGRAM_WINDOW_DEGREES} of the
     * peak bin (circular distance, so the 179°/0° wraparound is handled
     * correctly). Requires a minimum sample count so a nearly-blank
     * background (a handful of stray edge pixels) can't produce a
     * statistically meaningless "spread."
     */
    private double perspectiveSkewFromHistogram(int[] histogram) {
        int peakBin = peakBin(histogram);
        long weightInWindow = 0;
        double weightedSquaredDistance = 0;

        for (int bin = 0; bin < histogram.length; bin++) {
            int count = histogram[bin];
            if (count == 0) continue;
            int rawDistance = Math.abs(bin - peakBin);
            int circularDistance = Math.min(rawDistance, 180 - rawDistance);
            if (circularDistance > PERSPECTIVE_HISTOGRAM_WINDOW_DEGREES) continue;

            weightInWindow += count;
            weightedSquaredDistance += (double) count * circularDistance * circularDistance;
        }

        if (weightInWindow < PERSPECTIVE_MIN_EDGE_SAMPLES) {
            return 0; // not enough confident edge signal to say anything about its spread
        }
        return Math.sqrt(weightedSquaredDistance / weightInWindow);
    }

    /**
     * Glare detection: fraction of sampled pixels that are both very bright
     * (luminance) and nearly colorless (low HSB saturation) — the signature
     * of a blown-out highlight from flash/reflection off glossy packaging,
     * as opposed to a genuinely white/light-colored label background which
     * tends to still carry faint texture and isn't uniformly saturation-zero.
     */
    private double detectGlareFraction(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int stride = Math.max(1, Math.min(width, height) / 200); // subsample for speed on larger working copies
        long sampled = 0;
        long glarePixels = 0;

        float[] hsb = new float[3];
        for (int y = 0; y < height; y += stride) {
            for (int x = 0; x < width; x += stride) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb);
                Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
                double luminance = 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
                sampled++;
                if (luminance > GLARE_LUMINANCE_THRESHOLD && hsb[1] < GLARE_SATURATION_THRESHOLD) {
                    glarePixels++;
                }
            }
        }
        return sampled == 0 ? 0 : glarePixels / (double) sampled;
    }

    /**
     * Cropping heuristic: compares average edge energy in a thin strip along
     * each of the four borders against the image's overall average edge
     * energy. A label that's been cut off mid-content tends to show
     * content/edges running right up to and through the crop boundary,
     * pushing border-strip edge density close to (or above) the image-wide
     * average; a properly framed shot has comparatively flat, low-content
     * margins around the label. Returns the highest per-border ratio found.
     */
    private double detectBorderContentDensityRatio(double[] laplacian, int width, int height) {
        double overallMeanAbs = ImageMath.meanAbs(laplacian);
        if (overallMeanAbs == 0) {
            return 0;
        }
        int borderThickness = Math.max(2, Math.min(width, height) / 30);

        double top = meanAbsRegion(laplacian, width, height, 0, 0, width, borderThickness);
        double bottom = meanAbsRegion(laplacian, width, height, 0, height - borderThickness, width, borderThickness);
        double left = meanAbsRegion(laplacian, width, height, 0, 0, borderThickness, height);
        double right = meanAbsRegion(laplacian, width, height, width - borderThickness, 0, borderThickness, height);

        double maxRatio = 0;
        for (double borderMean : new double[]{top, bottom, left, right}) {
            maxRatio = Math.max(maxRatio, borderMean / overallMeanAbs);
        }
        return maxRatio;
    }

    private double meanAbsRegion(double[] values, int width, int height, int startX, int startY, int regionWidth, int regionHeight) {
        int endX = Math.min(width, startX + regionWidth);
        int endY = Math.min(height, startY + regionHeight);
        double sum = 0;
        long count = 0;
        for (int y = Math.max(0, startY); y < endY; y++) {
            for (int x = Math.max(0, startX); x < endX; x++) {
                sum += Math.abs(values[y * width + x]);
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }
}
