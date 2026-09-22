package com.legalmetrology.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Every timestamp is stored and computed in UTC; conversion to IST (or any
 * other officer-facing locale) happens only at the display boundary.
 */
public final class DateTimeUtil {

    public static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DateTimeUtil() {
    }

    public static Instant nowUtc() {
        return Instant.now();
    }

    public static String toIndiaDisplayString(Instant instant) {
        if (instant == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(instant, INDIA_ZONE).format(DISPLAY_FORMATTER);
    }

    public static String toIndiaDateString(Instant instant) {
        if (instant == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(instant, INDIA_ZONE).format(DATE_ONLY_FORMATTER);
    }

    public static boolean isExpired(Instant expiresAt) {
        return expiresAt == null || Instant.now().isAfter(expiresAt);
    }
}
