package com.legalmetrology.evidence.annotate;

import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.evidence.annotate.impl.AnnotatedImageServiceImpl;
import com.legalmetrology.vision.provider.VisionBoundingBox;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotatedImageServiceImplTest {

    private final AnnotatedImageServiceImpl service = new AnnotatedImageServiceImpl();

    @Test
    void annotatingWithNoBoxesReturnsAFullSizeCopyWithNoLegend() throws IOException {
        BufferedImage source = solidColor(400, 300, Color.WHITE);

        byte[] png = service.annotate(source, List.of());
        BufferedImage decoded = decode(png);

        assertThat(decoded.getWidth()).isEqualTo(400);
        assertThat(decoded.getHeight()).isEqualTo(300);
    }

    @Test
    void annotatingWithABoxDrawsTheSeverityColorIntoThatRegion() throws IOException {
        BufferedImage source = solidColor(400, 300, Color.WHITE);
        VisionBoundingBox box = new VisionBoundingBox(0.25, 0.25, 0.5, 0.5);
        AnnotationBox annotation = new AnnotationBox(box, "MRP", Severity.CRITICAL, BigDecimal.valueOf(0.9));

        byte[] png = service.annotate(source, List.of(annotation));
        BufferedImage decoded = decode(png);

        // Somewhere along the drawn rectangle's border a CRITICAL-red pixel must now exist —
        // the source was pure white, so any red-dominant pixel can only be the overlay.
        boolean foundRedStroke = false;
        int expectedX = (int) (0.25 * 400);
        for (int y = (int) (0.25 * 300); y < (int) (0.75 * 300); y++) {
            int rgb = decoded.getRGB(expectedX, y);
            Color pixel = new Color(rgb, true);
            if (pixel.getRed() > 180 && pixel.getGreen() < 100 && pixel.getBlue() < 100) {
                foundRedStroke = true;
                break;
            }
        }
        assertThat(foundRedStroke).isTrue();
    }

    @Test
    void cropReturnsExactlyTheRequestedRegionInPixels() throws IOException {
        BufferedImage source = solidColor(1000, 500, Color.WHITE);
        VisionBoundingBox box = new VisionBoundingBox(0.1, 0.2, 0.3, 0.4);

        byte[] png = service.crop(source, box);
        BufferedImage decoded = decode(png);

        assertThat(decoded.getWidth()).isEqualTo(300);
        assertThat(decoded.getHeight()).isEqualTo(200);
    }

    @Test
    void cropClampsAnOutOfBoundsBoxToTheSourceImage() throws IOException {
        BufferedImage source = solidColor(100, 100, Color.WHITE);
        VisionBoundingBox box = new VisionBoundingBox(0.9, 0.9, 0.5, 0.5);

        byte[] png = service.crop(source, box);
        BufferedImage decoded = decode(png);

        assertThat(decoded.getWidth()).isGreaterThan(0);
        assertThat(decoded.getHeight()).isGreaterThan(0);
        assertThat(decoded.getWidth()).isLessThanOrEqualTo(100);
        assertThat(decoded.getHeight()).isLessThanOrEqualTo(100);
    }

    @Test
    void sideBySidePlacesBothImagesInOneCanvasWiderThanEither() throws IOException {
        byte[] original = service.crop(solidColor(200, 150, Color.WHITE), new VisionBoundingBox(0, 0, 1, 1));
        byte[] annotated = service.annotate(solidColor(200, 150, Color.WHITE), List.of());

        byte[] combined = service.sideBySide(original, annotated);
        BufferedImage decoded = decode(combined);

        assertThat(decoded.getWidth()).isGreaterThan(200);
        assertThat(decoded.getHeight()).isEqualTo(150);
    }

    private BufferedImage solidColor(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    private BufferedImage decode(byte[] png) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(png));
    }
}
