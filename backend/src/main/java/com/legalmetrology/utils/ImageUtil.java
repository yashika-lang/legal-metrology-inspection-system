package com.legalmetrology.utils;

import com.legalmetrology.exception.BadRequestException;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Deterministic, dependency-free image helpers. Heavier AI-driven analysis
 * (blur/glare/rotation detection via OpenCV, label detection, etc.) belongs
 * to the future {@code vision} module — this class only provides the
 * cheap, synchronous primitives every upload needs immediately: dimension
 * reads, a content hash, and a perceptual hash for duplicate detection.
 */
public final class ImageUtil {

    private static final int HASH_SIZE = 8; // 8x8 average-hash -> 64-bit fingerprint

    private ImageUtil() {
    }

    public static BufferedImage read(InputStream inputStream) {
        try {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new BadRequestException("The uploaded file is not a readable image");
            }
            return image;
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read uploaded image: " + ex.getMessage());
        }
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    /**
     * Average-hash (aHash): downscale to 8x8 grayscale, compare each pixel
     * to the mean, emit one bit per pixel. Near-duplicate images (same
     * label photographed twice, recompressed, or lightly cropped) end up
     * with a small Hamming distance between hashes — the cheap first pass
     * for duplicate-image detection before escalating to a vision-model
     * similarity call.
     */
    public static String perceptualHash(BufferedImage source) {
        Image scaledImage = source.getScaledInstance(HASH_SIZE, HASH_SIZE, Image.SCALE_AREA_AVERAGING);
        BufferedImage grayscale = new BufferedImage(HASH_SIZE, HASH_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        grayscale.getGraphics().drawImage(scaledImage, 0, 0, null);

        int[] pixels = new int[HASH_SIZE * HASH_SIZE];
        long sum = 0;
        int index = 0;
        for (int y = 0; y < HASH_SIZE; y++) {
            for (int x = 0; x < HASH_SIZE; x++) {
                int gray = grayscale.getRaster().getSample(x, y, 0);
                pixels[index++] = gray;
                sum += gray;
            }
        }
        double mean = sum / (double) pixels.length;

        long hash = 0L;
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] >= mean) {
                hash |= (1L << i);
            }
        }
        return Long.toHexString(hash);
    }

    public static int hammingDistance(String hexHashA, String hexHashB) {
        long a = Long.parseUnsignedLong(hexHashA, 16);
        long b = Long.parseUnsignedLong(hexHashB, 16);
        return Long.bitCount(a ^ b);
    }
}
