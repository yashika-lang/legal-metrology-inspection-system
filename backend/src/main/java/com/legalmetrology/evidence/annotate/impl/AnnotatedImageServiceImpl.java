package com.legalmetrology.evidence.annotate.impl;

import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.evidence.annotate.AnnotatedImageService;
import com.legalmetrology.evidence.annotate.AnnotationBox;
import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.vision.provider.VisionBoundingBox;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class AnnotatedImageServiceImpl implements AnnotatedImageService {

    private static final int STROKE_WIDTH = 4;
    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 16);
    private static final int LEGEND_SWATCH = 14;
    private static final int LEGEND_PADDING = 10;

    /**
     * A full inspection photo can be 4000x3000px+. Found live: uploading
     * that as a losslessly-encoded PNG "annotated" export to Supabase
     * Storage took long enough to hit the 75-second storage timeout and
     * fail the entire evidence-generation request outright. Capping the
     * annotated export's longest edge here — still PNG, still lossless,
     * just fewer pixels — keeps the format's fidelity (no JPEG artifacts
     * on a legal evidence record) while cutting the encoded size enough
     * to upload reliably. 2000px is comfortably more than a reviewer
     * needs to read a bounding box's label/confidence badge.
     */
    private static final int MAX_ANNOTATED_DIMENSION = 2000;

    @Override
    public byte[] annotate(BufferedImage source, List<AnnotationBox> boxes) {
        BufferedImage scaledSource = downscale(source, MAX_ANNOTATED_DIMENSION);
        BufferedImage canvas = new BufferedImage(scaledSource.getWidth(), scaledSource.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(scaledSource, 0, 0, null);
        g.setStroke(new BasicStroke(STROKE_WIDTH));
        g.setFont(LABEL_FONT);

        for (AnnotationBox annotation : boxes) {
            drawBox(g, canvas.getWidth(), canvas.getHeight(), annotation);
        }
        if (!boxes.isEmpty()) {
            drawLegend(g, canvas.getWidth());
        }
        g.dispose();
        return toPng(canvas);
    }

    @Override
    public byte[] sideBySide(byte[] originalPng, byte[] annotatedPng) {
        BufferedImage original = decode(originalPng);
        BufferedImage annotated = decode(annotatedPng);
        int divider = 4;
        int height = Math.max(original.getHeight(), annotated.getHeight());
        int width = original.getWidth() + divider + annotated.getWidth();

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.drawImage(original, 0, 0, null);
        g.setColor(Color.DARK_GRAY);
        g.fillRect(original.getWidth(), 0, divider, height);
        g.drawImage(annotated, original.getWidth() + divider, 0, null);
        g.dispose();
        return toPng(canvas);
    }

    @Override
    public byte[] crop(BufferedImage source, VisionBoundingBox box) {
        int[] rect = toPixelRect(box, source.getWidth(), source.getHeight());
        BufferedImage cropped = source.getSubimage(rect[0], rect[1], rect[2], rect[3]);
        return toPng(cropped);
    }

    private void drawBox(Graphics2D g, int canvasWidth, int canvasHeight, AnnotationBox annotation) {
        Color color = severityColor(annotation.severity());
        int[] rect = toPixelRect(annotation.box(), canvasWidth, canvasHeight);

        g.setColor(color);
        g.drawRect(rect[0], rect[1], rect[2], rect[3]);

        String label = annotation.label()
                + (annotation.confidence() != null ? " (" + annotation.confidence().multiply(java.math.BigDecimal.valueOf(100)).intValue() + "%)" : "");
        FontMetrics metrics = g.getFontMetrics();
        int labelWidth = metrics.stringWidth(label) + 12;
        int labelHeight = metrics.getHeight() + 6;
        int labelY = Math.max(0, rect[1] - labelHeight);

        g.setColor(color);
        g.fillRect(rect[0], labelY, labelWidth, labelHeight);
        g.setColor(Color.WHITE);
        g.drawString(label, rect[0] + 6, labelY + metrics.getAscent() + 2);
    }

    private void drawLegend(Graphics2D g, int canvasWidth) {
        String[] labels = {"CRITICAL", "MAJOR", "MINOR"};
        Color[] colors = {severityColor(Severity.CRITICAL),
                severityColor(Severity.MAJOR),
                severityColor(Severity.MINOR)};

        FontMetrics metrics = g.getFontMetrics();
        int rowHeight = LEGEND_SWATCH + 6;
        int legendHeight = LEGEND_PADDING * 2 + rowHeight * labels.length;
        int legendWidth = 0;
        for (String label : labels) {
            legendWidth = Math.max(legendWidth, LEGEND_PADDING * 2 + LEGEND_SWATCH + 8 + metrics.stringWidth(label));
        }

        int x = canvasWidth - legendWidth - LEGEND_PADDING;
        int y = LEGEND_PADDING;

        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(x, y, legendWidth, legendHeight, 8, 8);

        for (int i = 0; i < labels.length; i++) {
            int rowY = y + LEGEND_PADDING + i * rowHeight;
            g.setColor(colors[i]);
            g.fillRect(x + LEGEND_PADDING, rowY, LEGEND_SWATCH, LEGEND_SWATCH);
            g.setColor(Color.WHITE);
            g.drawString(labels[i], x + LEGEND_PADDING + LEGEND_SWATCH + 8, rowY + LEGEND_SWATCH - 2);
        }
    }

    private Color severityColor(Severity severity) {
        if (severity == null) {
            return Color.GRAY;
        }
        return switch (severity) {
            case CRITICAL -> new Color(220, 38, 38);
            case MAJOR -> new Color(234, 140, 0);
            case MINOR -> new Color(202, 178, 0);
        };
    }

    /** Converts an image-relative [0,1] box to a pixel rect, clamped so it can never fall outside the source image. */
    private int[] toPixelRect(VisionBoundingBox box, int width, int height) {
        if (box == null) {
            return new int[]{0, 0, width, height};
        }
        int x = clamp((int) Math.round(box.x() * width), 0, width - 1);
        int y = clamp((int) Math.round(box.y() * height), 0, height - 1);
        int w = clamp((int) Math.round(box.w() * width), 1, width - x);
        int h = clamp((int) Math.round(box.h() * height), 1, height - y);
        return new int[]{x, y, w, h};
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** No-op if already within {@code maxDimension} on its longest edge — never upscales a smaller source. */
    private BufferedImage downscale(BufferedImage source, int maxDimension) {
        int width = source.getWidth();
        int height = source.getHeight();
        double scale = Math.min(1.0, (double) maxDimension / Math.max(width, height));
        if (scale >= 1.0) {
            return source;
        }
        int scaledWidth = Math.max(1, (int) Math.round(width * scale));
        int scaledHeight = Math.max(1, (int) Math.round(height * scale));
        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();
        return scaled;
    }

    private byte[] toPng(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to encode annotated image as PNG", ex);
        }
    }

    private BufferedImage decode(byte[] png) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
            if (image == null) {
                throw new BadRequestException("Not a readable PNG image");
            }
            return image;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to decode PNG image", ex);
        }
    }
}
