package com.legalmetrology.ocr.normalize.impl;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.ocr.normalize.OcrNormalizationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrNormalizationServiceImpl implements OcrNormalizationService {

    private static final Pattern CURRENCY_PREFIX = Pattern.compile(
            "(?i)\\b(rs\\.?|inr|₹|rupees?)\\s*");
    private static final Pattern NUMBER = Pattern.compile("[\\d,]+(?:\\.\\d+)?");

    private static final Pattern QUANTITY_WITH_UNIT = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]+)");

    private static final Pattern PHONE_DIGITS = Pattern.compile("\\d{6,}");
    private static final Pattern EMAIL = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PIN_CODE = Pattern.compile("\\b(\\d{6})\\b");

    private static final Map<String, String> UNIT_ALIASES = Map.ofEntries(
            Map.entry("g", "g"), Map.entry("gm", "g"), Map.entry("gms", "g"), Map.entry("gram", "g"), Map.entry("grams", "g"),
            Map.entry("kg", "kg"), Map.entry("kgs", "kg"), Map.entry("kilogram", "kg"), Map.entry("kilograms", "kg"),
            Map.entry("mg", "mg"), Map.entry("milligram", "mg"),
            Map.entry("ml", "ml"), Map.entry("mls", "ml"), Map.entry("millilitre", "ml"), Map.entry("milliliter", "ml"),
            Map.entry("l", "l"), Map.entry("lt", "l"), Map.entry("ltr", "l"), Map.entry("litre", "l"), Map.entry("liter", "l"),
            Map.entry("pc", "pcs"), Map.entry("pcs", "pcs"), Map.entry("piece", "pcs"), Map.entry("pieces", "pcs"),
            Map.entry("n", "N") // "N" (numbers of items) declarations, e.g. "10N" packs
    );

    private static final Map<String, Integer> MONTH_NAMES = Map.ofEntries(
            Map.entry("jan", 1), Map.entry("january", 1),
            Map.entry("feb", 2), Map.entry("february", 2),
            Map.entry("mar", 3), Map.entry("march", 3),
            Map.entry("apr", 4), Map.entry("april", 4),
            Map.entry("may", 5),
            Map.entry("jun", 6), Map.entry("june", 6),
            Map.entry("jul", 7), Map.entry("july", 7),
            Map.entry("aug", 8), Map.entry("august", 8),
            Map.entry("sep", 9), Map.entry("sept", 9), Map.entry("september", 9),
            Map.entry("oct", 10), Map.entry("october", 10),
            Map.entry("nov", 11), Map.entry("november", 11),
            Map.entry("dec", 12), Map.entry("december", 12)
    );

    @Override
    public String normalizeForDeclarationType(DeclarationType type, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return switch (type) {
            case MRP -> normalizeCurrency(rawValue);
            case NET_QUANTITY -> normalizeUnit(rawValue);
            case MFG_MONTH, MFG_YEAR -> normalizeMonthYear(rawValue);
            case EMAIL -> normalizeEmail(rawValue);
            case CUSTOMER_CARE -> {
                String phone = normalizePhone(rawValue);
                yield phone != null ? phone : normalizeWhitespace(rawValue);
            }
            default -> normalizeWhitespace(rawValue);
        };
    }

    @Override
    public String normalizeWhitespace(String rawValue) {
        return rawValue.replaceAll("\\p{Cntrl}", "").replaceAll("\\s+", " ").trim();
    }

    @Override
    public String normalizeCurrency(String rawValue) {
        String cleaned = CURRENCY_PREFIX.matcher(normalizeWhitespace(rawValue)).replaceAll("");
        Matcher matcher = NUMBER.matcher(cleaned);
        if (!matcher.find()) {
            return null;
        }
        String numberText = matcher.group().replace(",", "");
        BigDecimal amount = new BigDecimal(numberText);
        boolean hasFraction = numberText.contains(".");
        String formatted = hasFraction
                ? amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
                : amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
        return "₹" + formatted;
    }

    @Override
    public String normalizeUnit(String rawValue) {
        Matcher matcher = QUANTITY_WITH_UNIT.matcher(normalizeWhitespace(rawValue));
        if (!matcher.find()) {
            return normalizeWhitespace(rawValue);
        }
        String number = matcher.group(1);
        String unitToken = matcher.group(2).toLowerCase();
        String canonicalUnit = UNIT_ALIASES.get(unitToken);
        if (canonicalUnit == null && unitToken.endsWith("s")) {
            canonicalUnit = UNIT_ALIASES.get(unitToken.substring(0, unitToken.length() - 1));
        }
        return canonicalUnit != null ? number + canonicalUnit : number + unitToken;
    }

    /**
     * Handles numeric ("12/2025", "12-2025") and English month-name
     * ("Dec 2025", "December-25") formats. Regional-language month names
     * (Hindi/Marathi/Tamil/Gujarati) are not covered by this heuristic —
     * dates in those scripts pass through {@link #normalizeWhitespace}
     * unchanged rather than being silently misparsed.
     */
    @Override
    public String normalizeMonthYear(String rawValue) {
        String cleaned = normalizeWhitespace(rawValue);

        Matcher numeric = Pattern.compile("(\\d{1,2})[\\s/.\\-](\\d{4}|\\d{2})").matcher(cleaned);
        if (numeric.find()) {
            int month = Integer.parseInt(numeric.group(1));
            if (month >= 1 && month <= 12) {
                return "%04d-%02d".formatted(expandYear(numeric.group(2)), month);
            }
        }

        Matcher named = Pattern.compile("(?i)([a-zA-Z]+)[\\s.\\-]*(\\d{4}|\\d{2})").matcher(cleaned);
        if (named.find()) {
            Integer month = MONTH_NAMES.get(named.group(1).toLowerCase());
            if (month != null) {
                return "%04d-%02d".formatted(expandYear(named.group(2)), month);
            }
        }

        return cleaned;
    }

    @Override
    public String normalizePhone(String rawValue) {
        // Collapse internal spacing/hyphenation ("98765 43210", "98765-43210") before hunting
        // for the digit run — PHONE_DIGITS alone would otherwise see several short runs
        // instead of one number.
        String tightened = rawValue.replaceAll("[\\s\\-]+", "");
        Matcher matcher = PHONE_DIGITS.matcher(tightened);
        if (!matcher.find()) {
            return null;
        }
        String digits = matcher.group();
        if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        }
        if (digits.length() == 10) {
            return "+91" + digits;
        }
        return digits.length() >= 6 ? digits : null;
    }

    @Override
    public String normalizeEmail(String rawValue) {
        Matcher matcher = EMAIL.matcher(rawValue);
        return matcher.find() ? matcher.group().toLowerCase() : null;
    }

    @Override
    public String normalizePinCode(String rawValue) {
        Matcher matcher = PIN_CODE.matcher(rawValue);
        return matcher.find() ? matcher.group(1) : null;
    }

    private int expandYear(String yearText) {
        int year = Integer.parseInt(yearText);
        return year < 100 ? 2000 + year : year;
    }
}
