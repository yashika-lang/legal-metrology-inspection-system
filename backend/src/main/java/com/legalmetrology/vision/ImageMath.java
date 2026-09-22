package com.legalmetrology.vision;

import java.awt.image.BufferedImage;

/**
 * Small, dependency-free pixel-math primitives (grayscale conversion, Sobel
 * gradients, basic statistics) shared by every pure-Java image heuristic in
 * this module ({@code quality} and {@code font}) so the same well-tested
 * building blocks back every measurement instead of each analyzer
 * reimplementing its own.
 */
public final class ImageMath {

    private ImageMath() {
    }

    public static int[] toGrayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] gray = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                gray[y * width + x] = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            }
        }
        return gray;
    }

    public static double sobelGx(int[] gray, int width, int x, int y) {
        return (gray[(y - 1) * width + (x + 1)] + 2.0 * gray[y * width + (x + 1)] + gray[(y + 1) * width + (x + 1)])
                - (gray[(y - 1) * width + (x - 1)] + 2.0 * gray[y * width + (x - 1)] + gray[(y + 1) * width + (x - 1)]);
    }

    public static double sobelGy(int[] gray, int width, int x, int y) {
        return (gray[(y + 1) * width + (x - 1)] + 2.0 * gray[(y + 1) * width + x] + gray[(y + 1) * width + (x + 1)])
                - (gray[(y - 1) * width + (x - 1)] + 2.0 * gray[(y - 1) * width + x] + gray[(y - 1) * width + (x + 1)]);
    }

    public static double[] sobelMagnitude(int[] gray, int width, int height) {
        double[] magnitude = new double[width * height];
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                double gx = sobelGx(gray, width, x, y);
                double gy = sobelGy(gray, width, x, y);
                magnitude[y * width + x] = Math.sqrt(gx * gx + gy * gy);
            }
        }
        return magnitude;
    }

    public static double[] toDoubleArray(int[] values) {
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i];
        }
        return result;
    }

    public static double mean(double[] values) {
        if (values.length == 0) return 0;
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    public static double meanAbs(double[] values) {
        if (values.length == 0) return 0;
        double sum = 0;
        for (double v : values) sum += Math.abs(v);
        return sum / values.length;
    }

    public static double variance(double[] values) {
        if (values.length == 0) return 0;
        double mean = mean(values);
        double sumSquaredDiff = 0;
        for (double v : values) {
            double diff = v - mean;
            sumSquaredDiff += diff * diff;
        }
        return sumSquaredDiff / values.length;
    }

    public static double stdDev(double[] values) {
        return Math.sqrt(variance(values));
    }
}
