package com.legalmetrology.vision.quality;

import com.legalmetrology.vision.quality.impl.PureJavaImageQualityAnalyzer;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class PureJavaImageQualityAnalyzerTest {

    private final PureJavaImageQualityAnalyzer analyzer = new PureJavaImageQualityAnalyzer();

    @Test
    void aSharpHighContrastCheckerboardScoresWellWithNoBlurWarning() {
        BufferedImage image = checkerboard(1200, 1200, 20);

        QualityAnalysisResult result = analyzer.analyze(image);

        assertThat(result.warnings()).doesNotContain(QualityWarning.BLUR, QualityWarning.LOW_CONTRAST, QualityWarning.LOW_RESOLUTION);
        assertThat(result.score()).isGreaterThan(60);
    }

    @Test
    void aFlatUniformGrayImageIsFlaggedBlurryAndLowContrast() {
        BufferedImage image = solidColor(1200, 1200, new Color(128, 128, 128));

        QualityAnalysisResult result = analyzer.analyze(image);

        assertThat(result.warnings()).contains(QualityWarning.BLUR, QualityWarning.LOW_CONTRAST);
        assertThat(result.isRetakeRecommended()).isTrue();
    }

    @Test
    void aVerySmallImageIsFlaggedLowResolutionAndRecommendsRetake() {
        BufferedImage image = checkerboard(200, 200, 10);

        QualityAnalysisResult result = analyzer.analyze(image);

        assertThat(result.warnings()).contains(QualityWarning.LOW_RESOLUTION);
        assertThat(result.isRetakeRecommended()).isTrue();
    }

    @Test
    void aNearWhiteBlownOutImageIsFlaggedForGlare() {
        BufferedImage image = solidColor(1200, 1200, new Color(252, 252, 252));

        QualityAnalysisResult result = analyzer.analyze(image);

        assertThat(result.warnings()).contains(QualityWarning.GLARE);
    }

    @Test
    void aVeryDarkImageIsFlaggedLowBrightness() {
        BufferedImage image = solidColor(1200, 1200, new Color(10, 10, 10));

        QualityAnalysisResult result = analyzer.analyze(image);

        assertThat(result.warnings()).contains(QualityWarning.LOW_BRIGHTNESS);
    }

    private BufferedImage solidColor(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }

    private BufferedImage checkerboard(int width, int height, int squareSize) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        for (int y = 0; y < height; y += squareSize) {
            for (int x = 0; x < width; x += squareSize) {
                boolean black = ((x / squareSize) + (y / squareSize)) % 2 == 0;
                graphics.setColor(black ? Color.BLACK : Color.WHITE);
                graphics.fillRect(x, y, squareSize, squareSize);
            }
        }
        graphics.dispose();
        // Sprinkle a little noise so the checkerboard isn't a *perfect* signal in every metric.
        Random random = new Random(42);
        for (int i = 0; i < (width * height) / 500; i++) {
            image.setRGB(random.nextInt(width), random.nextInt(height), Color.GRAY.getRGB());
        }
        return image;
    }
}
