package com.legalmetrology.utils;

import java.util.UUID;
import java.util.regex.Pattern;

public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // EAN-8, UPC-A(12), EAN-13, ITF-14 — the barcode symbologies ZXing decodes for packaged goods
    private static final Pattern BARCODE_PATTERN = Pattern.compile("^\\d{8}(\\d{4,6})?$");

    private ValidationUtil() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidBarcode(String barcode) {
        return barcode != null && BARCODE_PATTERN.matcher(barcode).matches();
    }

    public static boolean isValidUuid(String value) {
        if (isBlank(value)) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
