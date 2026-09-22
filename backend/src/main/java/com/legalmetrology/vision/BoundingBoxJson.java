package com.legalmetrology.vision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalmetrology.vision.provider.VisionBoundingBox;

/**
 * (De)serializes {@link VisionBoundingBox} to/from the plain JSON text
 * stored in the {@code bounding_box} column on both {@code label_detections}
 * and {@code violations} (see docs/ARCHITECTURE.md — kept as {@code text}
 * rather than {@code jsonb} so it binds as an ordinary JDBC string, no
 * Postgres-side type coercion involved).
 */
public final class BoundingBoxJson {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private BoundingBoxJson() {
    }

    public static String toJson(VisionBoundingBox box) {
        if (box == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(box);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize bounding box", ex);
        }
    }

    public static VisionBoundingBox fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, VisionBoundingBox.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
