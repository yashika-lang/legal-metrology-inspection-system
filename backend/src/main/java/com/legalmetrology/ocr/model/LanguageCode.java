package com.legalmetrology.ocr.model;

/**
 * The languages this system's OCR pipeline targets. Carries the mapping to
 * each provider's own language identifier so provider adapters never
 * hardcode a language string inline.
 */
public enum LanguageCode {
    EN("en", "eng"),
    HI("hi", "hin"),
    MR("mr", "mar"),
    TA("ta", "tam"),
    GU("gu", "guj"),
    UNKNOWN("und", null);

    private final String googleVisionCode;
    private final String tesseractCode;

    LanguageCode(String googleVisionCode, String tesseractCode) {
        this.googleVisionCode = googleVisionCode;
        this.tesseractCode = tesseractCode;
    }

    public String googleVisionCode() {
        return googleVisionCode;
    }

    public String tesseractCode() {
        return tesseractCode;
    }

    /** All languages Tesseract should load, joined for its multi-language mode, e.g. "eng+hin+mar+tam+guj". */
    public static String tesseractCombinedLanguageString() {
        StringBuilder builder = new StringBuilder();
        for (LanguageCode code : values()) {
            if (code.tesseractCode == null) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('+');
            }
            builder.append(code.tesseractCode);
        }
        return builder.toString();
    }

    public static LanguageCode fromGoogleVisionCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        String normalized = code.toLowerCase().split("-")[0]; // Vision sometimes returns "en-US" style locales
        for (LanguageCode value : values()) {
            if (value.googleVisionCode.equals(normalized)) {
                return value;
            }
        }
        return UNKNOWN;
    }
}
