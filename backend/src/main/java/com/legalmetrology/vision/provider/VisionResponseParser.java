package com.legalmetrology.vision.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.LabelSection;
import com.legalmetrology.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the JSON contract defined by {@link VisionPrompts} into a
 * {@link VisionAnalysisResult}. Shared by every {@link VisionAiProvider} so
 * the response contract is defined and validated in exactly one place.
 */
@Slf4j
final class VisionResponseParser {

    private VisionResponseParser() {
    }

    static VisionAnalysisResult parse(String rawText, ObjectMapper objectMapper) {
        try {
            JsonNode root = objectMapper.readTree(stripMarkdownFences(rawText));
            LabelSection labelSection = parseLabelSection(root.path("labelSection").asText("UNKNOWN"));

            List<DetectedDeclaration> declarations = new ArrayList<>();
            for (JsonNode node : root.path("declarations")) {
                DetectedDeclaration declaration = parseDeclaration(node);
                if (declaration != null) {
                    declarations.add(declaration);
                }
            }
            return new VisionAnalysisResult(labelSection, declarations, null, 0);
        } catch (Exception ex) {
            log.error("Failed to parse Vision AI JSON response: {}", rawText, ex);
            throw new BadRequestException("Failed to parse Vision AI response: " + ex.getMessage());
        }
    }

    /** Some providers occasionally wrap JSON in ```json fences despite instructions not to — strip them if present. */
    private static String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\n?", "").replaceFirst("```\\s*$", "");
        }
        return trimmed.trim();
    }

    private static DetectedDeclaration parseDeclaration(JsonNode node) {
        String rawType = node.path("type").asText();
        DeclarationType type;
        try {
            // Defensive normalization: the prompt sends the exact enum names,
            // but an LLM occasionally returns a near-miss (different case, a
            // stray space, a hyphen instead of underscore) despite explicit
            // instructions. Without this, DeclarationType.valueOf() throws,
            // and the entire declaration for that type is silently dropped
            // for this image — a real, previously-unlogged-loudly way for a
            // visibly-present field to vanish before it ever reaches fusion.
            type = DeclarationType.valueOf(rawType.trim().toUpperCase().replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            log.error("Vision AI returned an unrecognized declaration type '{}' — this declaration is being dropped entirely for this image, not just marked absent. Full node: {}", rawType, node);
            return null;
        }

        VisionBoundingBox boundingBox = null;
        JsonNode boxNode = node.path("boundingBox");
        if (boxNode.isObject()) {
            boundingBox = new VisionBoundingBox(
                    boxNode.path("x").asDouble(), boxNode.path("y").asDouble(),
                    boxNode.path("w").asDouble(), boxNode.path("h").asDouble());
        }

        return new DetectedDeclaration(
                type,
                node.path("present").asBoolean(false),
                node.path("value").isNull() ? null : node.path("value").asText(null),
                node.path("confidence").asDouble(0.0),
                boundingBox
        );
    }

    private static LabelSection parseLabelSection(String value) {
        try {
            return LabelSection.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return LabelSection.UNKNOWN;
        }
    }
}
