package com.legalmetrology.ocr.language;

import com.legalmetrology.ocr.model.LanguageCode;

/**
 * Fallback language identification by Unicode script range, used whenever a
 * provider can't (or won't) report a detected language itself — Tesseract
 * has no reliable per-word language ID without a separate OSD pass, so
 * {@code TesseractOcrProvider} always uses this.
 * <p>
 * Known limitation: Hindi and Marathi both use the Devanagari script and
 * are not distinguishable by Unicode range alone; text in either language
 * is classified as {@link LanguageCode#HI} (the more common label of the
 * two in this domain). A real script-vs-language distinction would need a
 * statistical language model, out of scope for this heuristic.
 */
public final class ScriptBasedLanguageDetector {

    private ScriptBasedLanguageDetector() {
    }

    public static LanguageCode detect(String text) {
        if (text == null || text.isBlank()) {
            return LanguageCode.UNKNOWN;
        }

        int devanagari = 0, tamil = 0, gujarati = 0, latin = 0, total = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || Character.isDigit(c)) {
                continue;
            }
            total++;
            if (c >= 0x0900 && c <= 0x097F) {
                devanagari++;
            } else if (c >= 0x0B80 && c <= 0x0BFF) {
                tamil++;
            } else if (c >= 0x0A80 && c <= 0x0AFF) {
                gujarati++;
            } else if (Character.isLetter(c) && c < 0x0250) { // basic + extended Latin
                latin++;
            }
        }

        if (total == 0) {
            return LanguageCode.UNKNOWN;
        }

        int max = Math.max(Math.max(devanagari, tamil), Math.max(gujarati, latin));
        if (max == 0 || max < total * 0.3) {
            return LanguageCode.UNKNOWN;
        }
        if (max == devanagari) {
            return LanguageCode.HI;
        }
        if (max == tamil) {
            return LanguageCode.TA;
        }
        if (max == gujarati) {
            return LanguageCode.GU;
        }
        return LanguageCode.EN;
    }
}
