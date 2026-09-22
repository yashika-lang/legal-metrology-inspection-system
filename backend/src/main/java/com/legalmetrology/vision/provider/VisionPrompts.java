package com.legalmetrology.vision.provider;

import com.legalmetrology.common.enums.DeclarationType;

import java.util.Arrays;
import java.util.stream.Collectors;

/** The shared instruction text every {@link VisionAiProvider} sends — kept in one place so provider prompts never drift apart. */
final class VisionPrompts {

    private VisionPrompts() {
    }

    static final String SYSTEM_INSTRUCTION = """
            You are an expert Legal Metrology inspection assistant analyzing a photograph of a
            packaged commodity's label. Identify which of the following mandatory declarations
            are visibly present on THIS image, and where.

            Declaration types: %s

            For every declaration type in that list, report whether it is present on this image.
            If present, report its visible text value, your confidence (0.0-1.0), and a bounding
            box in IMAGE-RELATIVE coordinates (x, y, w, h each between 0 and 1, origin top-left,
            covering the text region). If not present or not legible on this image, still include
            it with present=false, value=null, confidence=your confidence that it is genuinely
            absent (not just off-frame), and boundingBox=null.

            Also classify which face of the package this image shows: FRONT (the primary display
            panel), BACK, SIDE, or UNKNOWN if you cannot tell.

            Respond with ONLY a single JSON object, no markdown fences, no commentary, exactly
            matching this shape:
            {
              "labelSection": "FRONT" | "BACK" | "SIDE" | "UNKNOWN",
              "declarations": [
                { "type": "MRP", "present": true, "value": "₹120", "confidence": 0.95,
                  "boundingBox": { "x": 0.12, "y": 0.55, "w": 0.3, "h": 0.06 } }
              ]
            }
            """;

    static String buildInstruction() {
        String types = Arrays.stream(DeclarationType.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        return SYSTEM_INSTRUCTION.formatted(types);
    }
}
