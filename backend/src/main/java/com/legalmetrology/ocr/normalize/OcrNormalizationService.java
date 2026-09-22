package com.legalmetrology.ocr.normalize;

import com.legalmetrology.common.enums.DeclarationType;

/**
 * Canonicalizes the raw text OCR/classification pulls off a label so
 * downstream comparison (rule evaluation, duplicate/fusion matching,
 * reporting) never has to deal with "Rs.120" vs "₹120" vs "INR 120" as
 * different values.
 */
public interface OcrNormalizationService {

    /** Dispatches to the right normalizer for the declaration type; falls back to whitespace-only cleanup for types with no specific format. */
    String normalizeForDeclarationType(DeclarationType type, String rawValue);

    /** Collapses runs of whitespace, trims, and strips control characters. */
    String normalizeWhitespace(String rawValue);

    /** "Rs.120", "₹120", "INR 120", "Rs 120.00" → "₹120.00". */
    String normalizeCurrency(String rawValue);

    /** "500 gm", "500gms", "0.5kg" → canonical unit abbreviations (g, kg, ml, l, ...). */
    String normalizeUnit(String rawValue);

    /** "12/2025", "Dec 2025", "December-25" → "2025-12" (month-year declarations only carry month+year). */
    String normalizeMonthYear(String rawValue);

    /** Strips separators/spaces down to digits, validates length, formats as a 10-digit Indian mobile or retains a landline/STD-coded number as-is. */
    String normalizePhone(String rawValue);

    String normalizeEmail(String rawValue);

    /** Validates and normalizes a 6-digit Indian PIN code; returns null if the input doesn't contain one. */
    String normalizePinCode(String rawValue);
}
