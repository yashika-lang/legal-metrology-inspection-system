package com.legalmetrology.ocr.classify.impl;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.ocr.classify.ClassifiedField;
import com.legalmetrology.ocr.classify.DeclarationClassifier;
import com.legalmetrology.ocr.model.LanguageCode;
import com.legalmetrology.ocr.model.OcrExtractionResult;
import com.legalmetrology.ocr.model.OcrLine;
import com.legalmetrology.ocr.model.OcrParagraph;
import com.legalmetrology.ocr.normalize.OcrNormalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keyword + regex based classifier: scans every OCR line for one of a fixed
 * set of trigger phrases (e.g. "net wt", "customer care", "fssai") and, on a
 * match, extracts and normalizes the value near that keyword. Two
 * declaration types have no reliable textual trigger of their own —
 * {@link DeclarationType#PRODUCT_NAME} and {@link DeclarationType#GENERIC_NAME}
 * are left for Vision AI (see {@code vision.service.VisionAiService}), which
 * can read them directly off the label's visual layout instead of guessing
 * from keywords.
 */
@Component
@RequiredArgsConstructor
public class DeclarationClassifierImpl implements DeclarationClassifier {

    private final OcrNormalizationService normalizationService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern CURRENCY_VALUE = Pattern.compile(
            "(?i)(?:rs\\.?|inr|₹)\\s*[\\d,]+(?:\\.\\d+)?|[\\d,]+(?:\\.\\d+)?");
    private static final Pattern QUANTITY_VALUE = Pattern.compile(
            "\\d+(?:\\.\\d+)?\\s*[a-zA-Z]+");
    private static final Pattern DATE_VALUE = Pattern.compile(
            "(?i)\\d{1,2}[\\s/.\\-]\\d{2,4}|[a-zA-Z]+[\\s.\\-]*\\d{2,4}");
    private static final Pattern CODE_VALUE = Pattern.compile("[A-Za-z0-9/\\-]{3,}");

    private record DeclarationRule(DeclarationType type, List<String> keywords, Pattern valuePattern, double baseConfidence) {
        boolean matches(String lowercaseLine) {
            return keywords.stream().anyMatch(lowercaseLine::contains);
        }
    }

    private final List<DeclarationRule> rules = List.of(
            new DeclarationRule(DeclarationType.MRP,
                    List.of("mrp", "m.r.p", "maximum retail price"), CURRENCY_VALUE, 0.85),
            new DeclarationRule(DeclarationType.NET_QUANTITY,
                    List.of("net wt", "net weight", "net qty", "net quantity", "net vol", "net volume", "contents"),
                    QUANTITY_VALUE, 0.85),
            new DeclarationRule(DeclarationType.MFG_MONTH,
                    List.of("mfg date", "mfg.", "mfd", "manufacturing date", "packed on", "pkd date", "date of manufacture"),
                    DATE_VALUE, 0.8),
            new DeclarationRule(DeclarationType.MANUFACTURER,
                    List.of("manufactured by", "mfd by", "mfg by", "manufacturer"), null, 0.75),
            new DeclarationRule(DeclarationType.PACKER,
                    List.of("packed by", "packer"), null, 0.75),
            new DeclarationRule(DeclarationType.IMPORTER,
                    List.of("imported by", "importer"), null, 0.75),
            new DeclarationRule(DeclarationType.ADDRESS,
                    List.of("address", "add:", "add."), null, 0.6),
            new DeclarationRule(DeclarationType.CUSTOMER_CARE,
                    List.of("customer care", "consumer care", "helpline", "toll free", "care no"), null, 0.75),
            new DeclarationRule(DeclarationType.LICENSE_NUMBER,
                    List.of("fssai", "lic no", "license no", "licence no"), CODE_VALUE, 0.85),
            new DeclarationRule(DeclarationType.BATCH_NUMBER,
                    List.of("batch no", "batch", "b.no", "lot no"), CODE_VALUE, 0.7),
            new DeclarationRule(DeclarationType.COUNTRY_OF_ORIGIN,
                    List.of("country of origin", "made in", "origin"), null, 0.7),
            new DeclarationRule(DeclarationType.GENERIC_NAME,
                    List.of("generic name", "common name"), null, 0.6)
    );

    @Override
    public List<ClassifiedField> classify(OcrExtractionResult extraction) {
        List<ClassifiedField> fields = new ArrayList<>();
        for (OcrParagraph paragraph : extraction.paragraphs()) {
            for (OcrLine line : paragraph.lines()) {
                fields.addAll(classifyLine(line, paragraph.language()));
            }
        }
        return fields;
    }

    private List<ClassifiedField> classifyLine(OcrLine line, LanguageCode language) {
        List<ClassifiedField> matches = new ArrayList<>();
        String lowercase = line.text().toLowerCase(Locale.ROOT);

        for (DeclarationRule rule : rules) {
            if (!rule.matches(lowercase)) {
                continue;
            }
            String value = extractValue(line.text(), rule);
            if (value == null || value.isBlank()) {
                continue;
            }
            double confidence = clamp(rule.baseConfidence() * line.confidence());
            String normalized = normalizationService.normalizeForDeclarationType(rule.type(), value);

            matches.add(new ClassifiedField(rule.type(), value, normalized, confidence, line.boundingBox(), language));
            if (rule.type() == DeclarationType.MFG_MONTH) {
                // The same date token establishes both the month and the year declaration.
                matches.add(new ClassifiedField(DeclarationType.MFG_YEAR, value, normalized, confidence, line.boundingBox(), language));
            }
        }

        Matcher emailMatcher = EMAIL_PATTERN.matcher(line.text());
        if (emailMatcher.find()) {
            String email = emailMatcher.group();
            matches.add(new ClassifiedField(DeclarationType.EMAIL, email, email.toLowerCase(Locale.ROOT),
                    clamp(0.9 * line.confidence()), line.boundingBox(), language));
        }

        return matches;
    }

    private String extractValue(String lineText, DeclarationRule rule) {
        if (rule.valuePattern() != null) {
            Matcher matcher = rule.valuePattern().matcher(lineText);
            if (matcher.find()) {
                return matcher.group();
            }
            return null;
        }
        // No structured pattern for this type — take whatever follows the matched keyword's separator.
        int separatorIndex = indexOfAny(lineText, ':', '-', '.');
        if (separatorIndex >= 0 && separatorIndex < lineText.length() - 1) {
            return lineText.substring(separatorIndex + 1).trim();
        }
        return lineText.trim();
    }

    private int indexOfAny(String text, char... candidates) {
        for (char candidate : candidates) {
            int index = text.indexOf(candidate);
            if (index >= 0) {
                return index;
            }
        }
        return -1;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
